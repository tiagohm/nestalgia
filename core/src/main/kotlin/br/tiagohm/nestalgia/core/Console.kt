package br.tiagohm.nestalgia.core

import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Console(
    val master: Console? = null,
    settings: EmulationSettings? = null,
) : Battery,
    Disposable,
    Snapshotable {

    private val pauseCounter = AtomicInteger(0)

    val batteryManager = BatteryManager(this)
    val notificationManager = NotificationManager()
    val debugger = Debugger(this)

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
        private set
    var slave: Console? = null
        private set

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
    private var running = false
    private var pauseOnNextFrameRequested = false
    private var resetRunTimers = false
    private var disableOcNextFrame = false
    private var initialized = false

    private val runLock = SimpleLock()
    private val stopLock = SimpleLock()
    private val clockTimer = Timer()
    private val lastFrameTimer = Timer()

    val settings: EmulationSettings = master?.settings ?: settings ?: EmulationSettings()

    var isPaused = false
        get() = master?.isPaused ?: field
        private set

    var emulationThreadId: Long = 0
        private set

    init {
        videoDecoder = VideoDecoder(this)
        videoRenderer = VideoRenderer(this)
        saveStateManager = SaveStateManager(this)
        cheatManager = CheatManager(this)
        soundMixer = SoundMixer(this)
        soundMixer.region = region

        if (master != null) {
            emulationThreadId = master.emulationThreadId
        }
    }

    override fun dispose() {
        debugger.dispose()

        stop()

        notificationManager.dispose()

        mapper?.dispose()

        videoDecoder.dispose()
        videoRenderer.dispose()
        soundMixer.dispose()

        if (::controlManager.isInitialized) {
            controlManager.dispose()
        }
    }

    fun release(forShutdown: Boolean) {
        slave?.release(true)
        slave = null

        if (forShutdown) {
            videoDecoder.stopThread()
            videoRenderer.stopThread()
        }

        master?.notificationManager?.sendNotification(NotificationType.VS_DUAL_SYSTEM_STOPPED)
    }

    override fun saveBattery() {
        if (mapper != null) {
            mapper!!.saveBattery()

            val device = controlManager.getControlDevice(ControlDevice.EXP_DEVICE_PORT)

            if (device is Battery) {
                device.saveBattery()
            }
        }
    }

    override fun loadBattery() {
        if (mapper != null) {
            mapper!!.loadBattery()

            val device = controlManager.getControlDevice(ControlDevice.EXP_DEVICE_PORT)

            if (device is Battery) {
                device.loadBattery()
            }
        }
    }

    fun initialize(rom: ByteArray, name: String, forPowerCycle: Boolean = false): Boolean {
        val (newMapper, data) = try {
            Mapper.initialize(this, rom, name)
        } catch (e: IOException) {
            notificationManager.sendNotification(NotificationType.ERROR, e.message)
            return false
        } catch (e: Exception) {
            System.err.println(e.message)
            return false
        } catch (e: Error) {
            System.err.println(e.message)
            return false
        }

        pause()

        soundMixer.stopAudio(true)

        if (mapper != null) {
            // Ensure we save any battery file before loading a new game
            saveBattery()
        }

        batteryManager.initialize()

        if (newMapper != null && data != null) {
            val isDifferentGame = mapper == null || mapper!!.info.hash.prgChrCrc32 != data.info.hash.prgChrCrc32

            if (mapper != null) {
                // Send notification only if a game was already running and
                // we successfully loaded the new one
                notificationManager.sendNotification(NotificationType.GAME_STOPPED)
            }

            videoDecoder.stopThread()

            val previousMapper = mapper
            mapper = newMapper
            memoryManager = MemoryManager(this)
            cpu = Cpu(this)
            apu = Apu(this)

            mapper!!.console = this
            mapper!!.initialize(data)

            if (previousMapper != null && !isDifferentGame && forPowerCycle) {
                mapper!!.copyPrgChrRom(previousMapper)
            }

            slave?.release(false)
            slave?.reset()

            if (master == null && mapper!!.info.vsType == VsSystemType.VS_DUAL_SYSTEM) {
                slave?.dispose()
                slave = Console(this)
                slave!!.initialize(rom, name)
            }

            when (mapper!!.info.system) {
                GameSystem.FDS -> {
                    settings.ppuModel = PpuModel.PPU_2C02
                    systemActionManager = FdsSystemActionManager(this, mapper!!)
                }
                GameSystem.VS_SYSTEM -> {
                    settings.ppuModel = mapper!!.info.vsPpuModel
                    systemActionManager = VsSystemActionManager(this)
                }
                else -> {
                    settings.ppuModel = PpuModel.PPU_2C02
                    systemActionManager = SystemActionManager(this)
                }
            }

            // Temporarely disable battery saves to prevent battery files from
            // being created for the wrong game (for Battle Box & Turbo File)
            batteryManager.isSaveEnabled = false

            var pollCounter = 0U

            if (::controlManager.isInitialized && !isDifferentGame) {
                // When power cycling, poll counter must be preserved to allow movies to playback properly
                pollCounter = controlManager.pollCounter
            }

            controlManager = if (mapper!!.info.system == GameSystem.VS_SYSTEM)
                VsControlManager(this, systemActionManager, mapper!!.controlDevice)
            else ControlManager(this, systemActionManager, mapper!!.controlDevice)

            controlManager.pollCounter = pollCounter
            controlManager.updateControlDevices()

            batteryManager.isSaveEnabled = true

            ppu = if (mapper is NsfMapper) NsfPpu(this) else Ppu(this)

            memoryManager.mapper = mapper
            memoryManager.registerIODevice(ppu)
            memoryManager.registerIODevice(apu)
            memoryManager.registerIODevice(controlManager)
            memoryManager.registerIODevice(mapper!!)

            region = Region.AUTO
            updateRegion(false)

            initialized = true

            resetComponents(false)

            // Poll controller input after creating rewind manager, to make sure it catches the first frame's input
            controlManager.updateInputState()

            videoDecoder.startThread()

            if (isMaster) {
                settings.clearFlag(EmulationFlag.FORCE_MAX_SPEED)

                if (slave != null) {
                    notificationManager.sendNotification(NotificationType.VS_DUAL_SYSTEM_STARTED)
                }
            }

            if (master != null) {
                notificationManager.sendNotification(NotificationType.GAME_INIT_COMPLETED)
            }

            resume()

            return true
        } else {
            resume()
        }

        debugger.resume()

        return false
    }

    fun processCpuClock() {
        mapper!!.processCpuClock()
        apu.processCpuClock()
    }

    val isDualSystem: Boolean
        get() = slave != null || master != null

    val dualConsole: Console?
        get() = slave ?: master

    inline val isMaster: Boolean
        get() = master == null

    inline val frameCount: Int
        get() = ppu.frameCount

    fun powerCycle() {
        reloadRom(true)
    }

    fun reloadRom(forPowerCycle: Boolean = false) {
        if (initialized && mapper != null) {
            initialize(mapper!!.data.bytes, mapper!!.name, forPowerCycle)
        }
    }

    fun reset(softReset: Boolean = true) {
        if (initialized) {
            val needSuspend = if (softReset) {
                systemActionManager.reset()
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
        slave?.resetComponents(softReset)

        soundMixer.stopAudio(true)
        memoryManager.reset(softReset)

        if (!settings.checkFlag(EmulationFlag.DISABLE_PPU_RESET) || !softReset || isNsf) {
            ppu.reset(softReset)
        }

        apu.reset(softReset)
        cpu.reset(softReset, region)
        controlManager.reset(softReset)
        soundMixer.reset(softReset)

        resetRunTimers = true

        if (master == null) {
            notificationManager.sendNotification(if (softReset) NotificationType.GAME_RESET else NotificationType.GAME_LOADED)
        }

        if (softReset) {
            debugger.resume()
        }
    }

    fun stop() {
        if (isRunning) {
            stop.set(true)

            debugger.suspend()

            stopLock.acquire()
            stopLock.release()
        }
    }

    fun pause() {
        if (master != null) {
            master.pause()
        } else {
            // Make sure debugger resumes if we try to pause the emu, otherwise we will get deadlocked.
            debugger.suspend()

            pauseCounter.incrementAndGet()
            runLock.acquire()
        }
    }

    fun resume() {
        if (master != null) {
            master.resume()
        } else {
            runLock.release()
            pauseCounter.decrementAndGet()

            // Make sure debugger resumes if we try to pause the emu, otherwise we will get deadlocked.
            debugger.resume()
        }
    }

    fun runSingleFrame() {
        val lastFrameNumber = ppu.frameCount
        emulationThreadId = Thread.currentThread().id

        updateRegion(true)

        while (ppu.frameCount == lastFrameNumber) {
            cpu.exec()

            if (slave != null) {
                runSlaveCpu()
            }
        }

        settings.disableOverclocking = disableOcNextFrame || isNsf
        disableOcNextFrame = false

        systemActionManager.processSystemActions()
        apu.endFrame()
    }

    fun runSlaveCpu() {
        while (true) {
            val cycleGap = cpu.cycleCount - slave!!.cpu.cycleCount

            if (cycleGap > 5L || ppu.frameCount > slave!!.ppu.frameCount) {
                slave!!.cpu.exec()
            } else {
                break
            }
        }
    }

    fun runFrame() {
        val frameCount = ppu.frameCount

        while (ppu.frameCount == frameCount) {
            cpu.exec()

            if (slave != null) {
                runSlaveCpu()
            }
        }
    }

    fun run() {
        if (mapper == null) {
            throw IllegalStateException("No mapper!")
        }

        clockTimer.reset()
        lastFrameTimer.reset()
        var lastDelay = frameDelay

        runLock.acquire()
        stopLock.acquire()

        emulationThreadId = Thread.currentThread().id

        slave?.emulationThreadId = Thread.currentThread().id

        var targetTime = lastDelay

        videoDecoder.startThread()

        updateRegion(true)

        running = true

        try {
            while (true) {
                runFrame()

                soundMixer.processEndOfFrame()
                slave?.soundMixer?.processEndOfFrame()

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
                    settings.setFlag(EmulationFlag.PAUSED)
                    pauseOnNextFrameRequested = false
                }

                var pausedRequired = settings.needsPause

                if (pausedRequired && !stop.get()) {
                    notificationManager.sendNotification(NotificationType.GAME_PAUSED)

                    // Prevent audio from looping endlessly while game is paused
                    soundMixer.stopAudio()
                    slave?.soundMixer?.stopAudio()

                    runLock.release()

                    while (pausedRequired && !stop.get()) {
                        Thread.sleep(30)
                        pausedRequired = settings.needsPause
                        isPaused = true
                    }

                    isPaused = false

                    runLock.acquire()
                    notificationManager.sendNotification(NotificationType.GAME_RESUMED)
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isPaused = false
        running = false

        notificationManager.sendNotification(NotificationType.BEFORE_EMULATION_STOP)

        videoDecoder.stopThread()
        soundMixer.stopAudio()

        settings.clearFlag(EmulationFlag.FORCE_MAX_SPEED)

        initialized = false

        if (mapper != null) {
            // Ensure we save any battery file before unloading anything
            saveBattery()
        }

        mapper?.dispose()
        mapper = null

        release(false)

        stopLock.release()
        runLock.release()

        emulationThreadId = Thread.currentThread().id

        notificationManager.sendNotification(NotificationType.GAME_STOPPED)
        notificationManager.sendNotification(NotificationType.EMULATION_STOPPED)
    }

    fun resetRunTimers() {
        resetRunTimers = true
    }

    val isRunning: Boolean
        get() = master?.isRunning ?: !stopLock.isFree && running

    val isStopped: Boolean
        get() = master?.isPaused ?: runLock.isFree || (!runLock.isFree && pauseCounter.get() > 0) || !running

    fun pauseOnNextFrame() {
        pauseOnNextFrameRequested = true
    }

    fun updateRegion(sendNotification: Boolean) {
        var configChanged = false

        if (settings.needControllerUpdate()) {
            controlManager.updateControlDevices()
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

            cpu.setMasterClockDivider(region)
            mapper!!.region = region
            ppu.region = region
            apu.region = region
        }

        if (configChanged && sendNotification) {
            notificationManager.sendNotification(NotificationType.CONFIG_CHANGED)
        }
    }

    val frameDelay: Double
        get() {
            val emulationSpeed = settings.getEmulationSpeed()

            return if (emulationSpeed == 0) {
                0.0
            } else {
                val delay = when (region) {
                    Region.PAL, Region.DENDY -> if (settings.checkFlag(EmulationFlag.INTEGER_FPS_MODE)) 20.0 else 19.99720920217466
                    else -> if (settings.checkFlag(EmulationFlag.INTEGER_FPS_MODE)) 16.6666666666666666667 else 16.63926405550947
                }

                delay / (emulationSpeed.toDouble() / 100)
            }
        }

    inline val fps: Double
        get() = if (region == Region.NTSC) {
            if (settings.checkFlag(EmulationFlag.INTEGER_FPS_MODE)) 60.0 else 60.098812
        } else {
            if (settings.checkFlag(EmulationFlag.INTEGER_FPS_MODE)) 50.0 else 50.006978
        }

    inline val lagCounter: UInt
        get() = controlManager.lagCounter

    inline fun resetLagCounter() {
        pause()
        controlManager.lagCounter = 0U
        resume()
    }

    fun setNextFrameOverclockStatus(disabled: Boolean) {
        disableOcNextFrame = disabled
    }

    fun initializeRam(ram: UByteArray) {
        initializeRam(settings.ramPowerOnState, ram)
    }

    private inline fun initializeRam(state: RamPowerOnState, ram: UByteArray) {
        when (state) {
            RamPowerOnState.ALL_ZEROS -> {
                ram.fill(0U)
            }
            RamPowerOnState.ALL_ONES -> {
                ram.fill(255U)
            }
            else -> {
                for (i in ram.indices) {
                    ram[i] = Random.nextInt(256).toUByte()
                }
            }
        }
    }

    inline val dipSwitchCount: Int
        get() = if (isVsSystem) {
            if (isDualSystem) 16 else 8
        } else if (mapper != null) {
            mapper!!.dipSwitchCount
        } else {
            0
        }

    inline val isNsf: Boolean
        get() = mapper is NsfMapper

    inline val isVsSystem: Boolean
        get() = controlManager is VsControlManager

    fun takeScreenshot(): IntArray {
        return if (isRunning && !isNsf) {
            return videoDecoder.takeScreenshot()
        } else {
            IntArray(0)
        }
    }

    override fun saveState(s: Snapshot) {
        if (isRunning) {
            // Send any unprocessed sound to the SoundMixer
            apu.endFrame()

            s.write("cpu", cpu)
            s.write("ppu", ppu)
            s.write("memoryManager", memoryManager)
            s.write("apu", apu)
            s.write("controlManager", controlManager)
            s.write("mapper", mapper!!)
            slave?.let { s.write("slave", it) }
        }
    }

    override fun restoreState(s: Snapshot) {
        if (isRunning) {
            s.load()

            // Send any unprocessed sound to the SoundMixer
            apu.endFrame()

            s.readSnapshot("cpu")?.let { cpu.restoreState(it) }
            s.readSnapshot("ppu")?.let { ppu.restoreState(it) }
            s.readSnapshot("memoryManager")?.let { memoryManager.restoreState(it) }
            s.readSnapshot("apu")?.let { apu.restoreState(it) }
            s.readSnapshot("controlManager")?.let { controlManager.restoreState(it) }
            s.readSnapshot("mapper")?.let { mapper!!.restoreState(it) }

            if (slave != null) s.readSnapshot("slave")?.let { slave!!.restoreState(it) }

            updateRegion(false)
        }
    }

    val availableFeatures: List<ConsoleFeature>
        get() {
            return if (mapper != null && ::controlManager.isInitialized) {
                val res = ArrayList<ConsoleFeature>(4)

                res.addAll(mapper!!.availableFeatures)

                if (controlManager is VsControlManager) {
                    res.add(ConsoleFeature.VS_SYSTEM)
                }

                // TODO: BarcodeReader e FamilyBasicDataRecorder

                res
            } else {
                emptyList()
            }
        }
}