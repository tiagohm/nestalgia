package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.EmulationFlag.*
import br.tiagohm.nestalgia.core.NotificationType.*
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class Console(val settings: EmulationSettings) : Battery, Resetable, Closeable, Snapshotable, Runnable {

    private val pauseCounter = AtomicInteger(0)

    val batteryManager = BatteryManager(this)
    val notificationManager = NotificationManager()

    @JvmField internal val debugger = Debugger(this)

    lateinit var cpu: Cpu
        private set

    lateinit var ppu: Ppu
        private set

    lateinit var apu: Apu
        private set

    lateinit var controlManager: ControlManager
        private set

    lateinit var memoryManager: MemoryManager
        private set

    lateinit var systemActionManager: SystemActionManager
        private set

    var mapper: Mapper? = null
        internal set

    var videoDecoder: VideoDecoder
        private set

    var videoRenderer: VideoRenderer
        private set

    var saveStateManager: SaveStateManager
        private set

    var cheatManager: CheatManager
        private set

    var soundMixer: SoundMixer
        private set

    var keyManager: KeyManager? = null

    var region = Region.NTSC
        private set

    private var stop = AtomicBoolean(false)
    private var mRunning = false
    private var pauseOnNextFrameRequested = false
    private var resetRunTimers = false
    private var disableOcNextFrame = false
    private var initialized = false

    private val runLock = SimpleLock()
    private val stopLock = SimpleLock()
    private val clockTimer = Timer()
    private val lastFrameTimer = Timer()

    var paused = false
        private set

    var emulationThreadId = 0L
        private set

    init {
        videoDecoder = VideoDecoder(this)
        videoRenderer = VideoRenderer(this)
        saveStateManager = SaveStateManager(this)
        cheatManager = CheatManager(this)
        soundMixer = SoundMixer(this)
        soundMixer.updateRegion(region)
    }

    override fun close() {
        debugger.close()

        stop()

        notificationManager.close()

        mapper?.close()

        videoDecoder.close()
        videoRenderer.close()
        soundMixer.close()

        if (::controlManager.isInitialized) {
            controlManager.close()
        }
    }

    fun release(forShutdown: Boolean) {
        if (forShutdown) {
            videoDecoder.stopThread()
            videoRenderer.stopThread()
        }
    }

    override fun saveBattery() {
        mapper?.saveBattery()

        val device = controlManager.controlDevice(ControlDevice.EXP_DEVICE_PORT)

        if (device is Battery) {
            device.saveBattery()
        }
    }

    override fun loadBattery() {
        mapper?.loadBattery()

        val device = controlManager.controlDevice(ControlDevice.EXP_DEVICE_PORT)

        if (device is Battery) {
            device.loadBattery()
        }
    }

    fun initialize(
        rom: IntArray,
        name: String,
        forPowerCycle: Boolean = false,
        fdsBios: IntArray = IntArray(0),
    ): Boolean {
        val previousMapper = mapper

        if (previousMapper != null) {
            // Ensure we save any battery file before loading a new game.
            saveBattery()
        }

        val newMapper = try {
            Mapper.initialize(this, rom, name, fdsBios)
        } catch (e: Throwable) {
            LOG.error("Failed to initialize mapper", e)
            notificationManager.sendNotification(ERROR, e.message)
            return false
        }

        pause()

        soundMixer.stopAudio(true)

        batteryManager.initialize()

        val isDifferentGame = previousMapper == null || previousMapper.info.hash.crc32 != newMapper.info.hash.crc32

        if (previousMapper != null) {
            // Send notification only if a game was already running and
            // we successfully loaded the new one
            notificationManager.sendNotification(GAME_STOPPED)
        }

        videoDecoder.stopThread()

        memoryManager = MemoryManager(this)
        cpu = Cpu(this)
        apu = Apu(this)

        val info = newMapper.info
        LOG.info(
            "{}, mapper={}, id={} crc={}, md5={}, sha1={}, sha256={}",
            info.name, newMapper::class.simpleName, info.mapperId, info.hash.crc32,
            info.hash.md5, info.hash.sha1, info.hash.sha256,
        )

        if (previousMapper != null && !isDifferentGame && forPowerCycle) {
            newMapper.copyPrgChrRom(previousMapper)
        }

        when (newMapper.info.system) {
            GameSystem.FDS -> {
                settings.ppuModel = PpuModel.PPU_2C02
                systemActionManager = FdsSystemActionManager(this, newMapper as Fds)
            }
            else -> {
                settings.ppuModel = PpuModel.PPU_2C02
                systemActionManager = SystemActionManager(this)
            }
        }

        // Temporarely disable battery saves to prevent battery files from
        // being created for the wrong game (for Battle Box & Turbo File)
        batteryManager.saveEnabled = false

        var pollCounter = 0

        if (::controlManager.isInitialized && !isDifferentGame) {
            // When power cycling, poll counter must be preserved to allow movies to playback properly
            pollCounter = controlManager.pollCounter
        }

        if (newMapper.info.system == GameSystem.VS_SYSTEM) {
            throw UnsupportedOperationException("VS. Dual System is not supported")
        }

        controlManager = ControlManager(this)

        batteryManager.saveEnabled = true

        ppu = if (newMapper is NsfMapper) NsfPpu(this) else Ppu(this)

        controlManager.pollCounter = pollCounter
        controlManager.updateControlDevices(true)

        newMapper.initialize()

        memoryManager.mapper = newMapper
        memoryManager.registerIODevice(ppu)
        memoryManager.registerIODevice(apu)
        memoryManager.registerIODevice(controlManager)
        memoryManager.registerIODevice(newMapper)

        region = Region.AUTO
        updateRegion(false)

        initialized = true

        resetComponents(false)

        // Poll controller input after creating rewind manager,
        // to make sure it catches the first frame's input.
        controlManager.updateInputState()

        videoDecoder.startThread()

        settings.flag(FORCE_MAX_SPEED, false)

        notificationManager.sendNotification(GAME_INIT_COMPLETED)

        if (isDifferentGame) {
            cheatManager.clear()
        }

        resume()

        return true
    }

    fun processCpuClock() {
        mapper!!.processCpuClock()
        apu.processCpuClock()
    }

    val frameCount
        get() = ppu.frameCount

    fun powerCycle() {
        reloadRom(true)
    }

    fun reloadRom(forPowerCycle: Boolean = false) {
        if (initialized && mapper != null) {
            initialize(
                mapper!!.data.rawData,
                mapper!!.name,
                forPowerCycle,
                mapper!!.data.fdsBios,
            )
        }
    }

    override fun reset(softReset: Boolean) {
        if (initialized) {
            val needSuspend = if (softReset) {
                systemActionManager.softReset()
            } else {
                systemActionManager.powerCycle()
            }

            // Only do this if a reset/power cycle is not already pending - otherwise we'll end up calling Suspend() too many times
            // Resume from code break if needed (otherwise reset doesn't happen right away)
            if (needSuspend) {
                debugger.suspend()
                debugger.run()
            }
        }
    }

    fun resetComponents(softReset: Boolean) {
        soundMixer.stopAudio(true)
        memoryManager.reset(softReset)

        ppu.reset(softReset)

        apu.reset(softReset)
        cpu.reset(softReset, region)
        controlManager.reset(softReset)
        soundMixer.reset(softReset)

        resetRunTimers = true

        notificationManager.sendNotification(if (softReset) GAME_RESET else GAME_LOADED)

        if (softReset) {
            debugger.resume()
        }
    }

    fun stop() {
        if (running) {
            stop.set(true)

            debugger.suspend()

            stopLock.acquire()
            stopLock.release()
        }
    }

    fun pause() {
        // Make sure debugger resumes if we try to pause the emulator,
        // otherwise we will get deadlocked.
        debugger.suspend()

        pauseCounter.incrementAndGet()
        runLock.acquire()
    }

    fun resume() {
        runLock.release()
        pauseCounter.decrementAndGet()

        // Make sure debugger resumes if we try to pause the emulator,
        // otherwise we will get deadlocked.
        debugger.resume()
    }

    fun runSingleFrame() {
        val lastFrameNumber = ppu.frameCount
        emulationThreadId = Thread.currentThread().id

        updateRegion(true)

        while (ppu.frameCount == lastFrameNumber) {
            cpu.exec()
        }

        settings.disableOverclocking = disableOcNextFrame || isNsf
        disableOcNextFrame = false

        systemActionManager.processSystemActions()
        apu.endFrame()
    }

    fun runFrame() {
        val frameCount = ppu.frameCount

        while (ppu.frameCount == frameCount) {
            cpu.exec()
        }
    }

    override fun run() {
        if (mapper == null) {
            throw IllegalStateException("No mapper!")
        }

        clockTimer.reset()
        lastFrameTimer.reset()
        var lastDelay = frameDelay

        runLock.acquire()
        stopLock.acquire()

        emulationThreadId = Thread.currentThread().id

        var targetTime = lastDelay

        videoDecoder.startThread()

        updateRegion(true)

        mRunning = true

        try {
            while (true) {
                runFrame()

                soundMixer.processEndOfFrame()
                controlManager.processEndOfFrame()

                settings.disableOverclocking = disableOcNextFrame || isNsf
                disableOcNextFrame = false

                updateRegion(true)

                val delay = frameDelay

                if (resetRunTimers || delay != lastDelay || (clockTimer.elapsedMilliseconds - targetTime) > 300) {
                    //Reset the timers, this can happen in 3 scenarios:
                    //1) Target frame rate changed
                    //2) The console was reset/power cycled or the emulation was paused (with or without the debugger)
                    //3) As a satefy net, if we overshoot our target by over 300 milliseconds, the timer is reset, too.
                    //   This can happen when something slows the emulator down severely (or when breaking execution in VS when debugging Mesen itself, etc.)
                    clockTimer.reset()
                    targetTime = 0.0

                    resetRunTimers = false
                    lastDelay = delay
                }

                targetTime += delay

                // When sleeping for a long time (e.g <= 25% speed), sleep in small chunks and check to see if we need to stop sleeping between each sleep call
                while (targetTime - clockTimer.elapsedMilliseconds > 50) {
                    clockTimer.waitUntil(clockTimer.elapsedMilliseconds + 40)

                    if (delay != frameDelay || stop.get() || settings.needsPause || pauseCounter.get() > 0) {
                        targetTime = 0.0
                        break
                    }
                }

                // Sleep until we're ready to start the next frame
                clockTimer.waitUntil(targetTime.toLong())

                if (pauseCounter.get() > 0) {
                    // Need to temporarely pause the emu (to save/load a state, etc.)
                    runLock.release()
                    // Spin wait until we are allowed to start again
                    while (pauseCounter.get() > 0);

                    runLock.acquire()
                }

                if (pauseOnNextFrameRequested) {
                    settings.flag(PAUSED, true)
                    pauseOnNextFrameRequested = false
                }

                var pausedRequired = settings.needsPause

                if (pausedRequired && !stop.get()) {
                    notificationManager.sendNotification(GAME_PAUSED)

                    // Prevent audio from looping endlessly while game is paused
                    soundMixer.stopAudio()

                    runLock.release()

                    while (pausedRequired && !stop.get()) {
                        Thread.sleep(30)
                        pausedRequired = settings.needsPause
                        paused = true
                    }

                    paused = false

                    runLock.acquire()
                    notificationManager.sendNotification(GAME_RESUMED)
                    lastFrameTimer.reset()

                    // Reset the timer to avoid speed up after a pause
                    resetRunTimers = true
                }

                systemActionManager.processSystemActions()

                if (stop.get()) {
                    stop.set(false)
                    break
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        paused = false
        mRunning = false

        notificationManager.sendNotification(BEFORE_EMULATION_STOP)

        videoDecoder.stopThread()
        soundMixer.stopAudio()

        settings.flag(FORCE_MAX_SPEED, false)

        initialized = false

        if (mapper != null) {
            // Ensure we save any battery file before unloading anything
            saveBattery()
        }

        mapper?.close()
        mapper = null

        release(false)

        stopLock.release()
        runLock.release()

        emulationThreadId = Thread.currentThread().id

        notificationManager.sendNotification(GAME_STOPPED)
        notificationManager.sendNotification(EMULATION_STOPPED)
    }

    fun resetRunTimers() {
        resetRunTimers = true
    }

    val running
        get() = !stopLock.isFree && mRunning

    val stopped
        get() = runLock.isFree || (!runLock.isFree && pauseCounter.get() > 0) || !mRunning

    fun pauseOnNextFrame() {
        pauseOnNextFrameRequested = true
    }

    fun updateRegion(sendNotification: Boolean) {
        var configChanged = false

        if (settings.needControllerUpdate()) {
            controlManager.updateControlDevices(true)
            configChanged = true
        }

        var region = settings.region

        if (region == Region.AUTO) {
            region = when (mapper!!.info.system) {
                GameSystem.PAL -> Region.PAL
                GameSystem.DENDY -> Region.DENDY
                else -> Region.NTSC
            }
        }

        if (this.region != region) {
            this.region = region

            configChanged = true

            cpu.masterClockDivider(region)
            mapper!!.updateRegion(region)
            ppu.updateRegion(region)
            apu.updateRegion(region)
            soundMixer.updateRegion(region)
        }

        if (configChanged && sendNotification) {
            notificationManager.sendNotification(CONFIG_CHANGED)
        }
    }

    val frameDelay: Double
        get() {
            val emulationSpeed = settings.emulationSpeed()

            return if (emulationSpeed == 0) {
                0.0
            } else {
                val delay = when (region) {
                    Region.PAL,
                    Region.DENDY -> if (settings.flag(INTEGER_FPS_MODE)) 20.0 else 19.99720920217466
                    else -> if (settings.flag(INTEGER_FPS_MODE)) 16.666666666666668 else 16.63926405550947
                }

                delay / (emulationSpeed.toDouble() / 100)
            }
        }

    val fps
        get() = if (region == Region.NTSC) {
            if (settings.flag(INTEGER_FPS_MODE)) 60.0 else 60.098812
        } else {
            if (settings.flag(INTEGER_FPS_MODE)) 50.0 else 50.006978
        }

    val lagCounter
        get() = controlManager.lagCounter

    fun resetLagCounter() {
        pause()
        controlManager.lagCounter = 0
        resume()
    }

    fun nextFrameOverclockStatus(disabled: Boolean) {
        disableOcNextFrame = disabled
    }

    fun initializeRam(ram: IntArray) {
        initializeRam(settings.ramPowerOnState, ram)
    }

    private fun initializeRam(state: RamPowerOnState, ram: IntArray) {
        when (state) {
            RamPowerOnState.ALL_ZEROS -> ram.fill(0)
            RamPowerOnState.ALL_ONES -> ram.fill(255)
            else -> repeat(ram.size) { ram[it] = Random.nextInt(256) }
        }
    }

    val dipSwitchCount
        get() = mapper?.dipSwitchCount ?: 0

    inline val masterClock
        get() = cpu.cycleCount

    val isNsf
        get() = mapper is NsfMapper

    val isFds
        get() = mapper is Fds

    val canScreenshot
        get() = running && !isNsf

    fun takeScreenshot(): IntArray {
        return if (canScreenshot) videoDecoder.takeScreenshot()
        else IntArray(0)
    }

    override fun saveState(s: Snapshot) {
        if (running) {
            // Send any unprocessed sound to the SoundMixer.
            apu.endFrame()

            s.write("cpu", cpu)
            s.write("ppu", ppu)
            s.write("memoryManager", memoryManager)
            s.write("apu", apu)
            s.write("controlManager", controlManager)
            s.write("mapper", mapper!!)
        }
    }

    override fun restoreState(s: Snapshot) {
        if (running) {
            // Send any unprocessed sound to the SoundMixer.
            apu.endFrame()

            s.readSnapshotable("cpu", cpu)
            s.readSnapshotable("ppu", ppu)
            s.readSnapshotable("memoryManager", memoryManager)
            s.readSnapshotable("apu", apu)
            s.readSnapshotable("controlManager", controlManager)
            s.readSnapshotable("mapper", mapper!!)

            updateRegion(false)
        }
    }

    companion object {

        @JvmStatic private val LOG = LoggerFactory.getLogger(Console::class.java)
    }
}
