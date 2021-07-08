package br.tiagohm.nestalgia.core

import kotlin.concurrent.thread

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
open class Emulator(
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
    }

    fun load(rom: ByteArray, name: String, fdsBios: ByteArray = ByteArray(0)): Boolean {
        return if (console.initialize(rom, name, true, fdsBios)) {
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

    inline val isPaused: Boolean
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

    inline val isVsSystem: Boolean
        get() = console.isVsSystem

    inline val isFds: Boolean
        get() = console.isFds

    inline val isNsf: Boolean
        get() = console.isNsf

    inline val isDualSystem: Boolean
        get() = console.isDualSystem

    val vsSystemActionManager: VsSystemActionManager?
        get() = if (!console.isRunning) null else console.systemActionManager as? VsSystemActionManager

    val fdsSystemActionManager: FdsSystemActionManager?
        get() = if (!console.isRunning) null else console.systemActionManager as? FdsSystemActionManager

    fun getControllerKey(port: Int) = console.settings.getControllerKeys(port)

    fun getControllerType(port: Int) = console.settings.getControllerType(port)

    @Suppress("UNCHECKED_CAST")
    fun <T : ControlDevice> getControlDevice(port: Int): T? = console.controlManager.getControlDevice(port) as? T

    fun insertCoin(port: Int) {
        vsSystemActionManager?.insertCoin(port)
    }

    val fdsSideCount: Int
        get() = fdsSystemActionManager?.sideCount ?: 0

    fun insertDisk(diskNumber: Int) {
        fdsSystemActionManager?.insertDisk(diskNumber)
    }

    fun switchDiskSide() {
        fdsSystemActionManager?.switchDiskSide()
    }

    fun ejectDisk() {
        fdsSystemActionManager?.ejectDisk()
    }

    inline val dipSwitchCount: Int
        get() = console.dipSwitchCount

    inline val info: RomInfo
        get() = console.mapper!!.info

    fun debugRunFrame(count: Int = 1) {
        if (isRunning) {
            console.debugger.frameStep(count)
        }
    }

    fun debugRunScanline(count: Int = 1) {
        if (isRunning) {
            console.debugger.scanlineStep(count)
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