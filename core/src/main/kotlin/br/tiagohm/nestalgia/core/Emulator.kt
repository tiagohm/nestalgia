package br.tiagohm.nestalgia.core

import kotlin.concurrent.thread

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Emulator(
    val console: Console,
    val audio: AudioDevice,
    val video: RenderingDevice,
    val keyManager: KeyManager,
    val inputProviders: Iterable<InputProvider>,
) : NotificationListener,
    Disposable {

    private var emuThread: Thread? = null

    val settings = console.settings

    inline val isRunning: Boolean
        get() = console.isRunning

    init {
        console.videoRenderer.registerRenderingDevice(video)
        console.soundMixer.registerAudioDevice(audio)
        console.keyManager = keyManager

        console.notificationManager.registerNotificationListener(this)
    }

    override fun dispose() {
        console.dispose()
        emuThread?.interrupt()
        emuThread = null
    }

    override fun processNotification(type: NotificationType, vararg data: Any?) {
        // nada
    }

    fun load(rom: ByteArray, name: String): Boolean {
        return if (console.initialize(rom, name, true)) {
            if (emuThread == null) {
                emuThread = thread(true, name = "Emulation", block = console::run)
            }

            inputProviders.forEach { console.controlManager.registerInputProvider(it) }

            true
        } else {
            false
        }
    }

    fun pause() {
        if (isRunning) {
            settings.setFlag(EmulationFlag.PAUSED)
        }
    }

    val isPaused: Boolean
        get() = isRunning && settings.checkFlag(EmulationFlag.PAUSED)

    fun resume() {
        if (isRunning) {
            settings.clearFlag(EmulationFlag.PAUSED)
        }
    }

    fun stop() {
        console.stop()
        emuThread?.interrupt()
        emuThread = null
    }

    fun reset() {
        if (console.debugger.isExecutionStopped) {
            console.resetComponents(true)
            console.controlManager.updateInputState()
            console.debugger.cpuStep(1)
        } else {
            console.reset(true)
        }
    }

    fun powerCycle() {
        if (console.debugger.isExecutionStopped) {
            console.powerCycle()
            console.debugger.cpuStep(1)
        } else {
            console.reset(false)
        }
    }

    fun reloadRom() {
        console.reloadRom()
    }

    fun takeScreenshot(): IntArray {
        return console.takeScreenshot()
    }

    fun saveState(): ByteArray {
        return console.saveStateManager.saveState()
    }

    fun restoreState(data: ByteArray) {
        console.saveStateManager.restoreState(data)
    }

    val isVsSystem: Boolean
        get() = console.isVsSystem

    val isDualSystem: Boolean
        get() = console.isDualSystem

    fun insertCoin(port: Int) {
        val sam = console.systemActionManager

        if (sam is VsSystemActionManager) {
            sam.insertCoin(port)
        }
    }

    val dipSwitchCount: Int
        get() = console.dipSwitchCount

    val info: RomInfo
        get() = console.mapper!!.info

    fun debugRunFrame(count: Int = 1) {
        if (isRunning) {
            val extraScanlines = settings.extraScanlinesAfterNmi + settings.extraScanlinesBeforeNmi
            val cycleCount = ((if (settings.region == Region.NTSC) 262 else 312) + extraScanlines) * 341
            console.debugger.ppuStep(count * cycleCount)
        }
    }

    fun debugRunScanline(count: Int = 1) {
        if (isRunning) {
            console.debugger.ppuStep(count * 341)
        }
    }

    fun debugRunPpuCycle(count: Int = 1) {
        if (isRunning) {
            console.debugger.ppuStep(count)
        }
    }

    fun debugRunCpuCycle(count: Int = 1) {
        if (isRunning) {
            console.debugger.cpuStep(count)
        }
    }

    fun debugRun() {
        if (isRunning) {
            console.debugger.run()
        }
    }

    fun debugStep(count: Int) {
        if (isRunning) {
            console.debugger.step(count)
        }
    }
}