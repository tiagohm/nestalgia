package br.tiagohm.nestalgia.core

import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

data class Emulator(
    @JvmField val console: Console,
    @JvmField val audio: AudioDevice,
    @JvmField val video: RenderingDevice,
    @JvmField val keyManager: KeyManager,
    @JvmField val inputProviders: Iterable<InputProvider>,
    @JvmField val threadExecutor: ExecutorService,
) : NotificationListener, Resetable, AutoCloseable {

    private val emuThread = AtomicReference<Future<*>>()

    inline val settings
        get() = console.settings

    inline val running
        get() = console.isRunning

    init {
        console.videoRenderer.registerRenderingDevice(video)
        console.soundMixer.registerAudioDevice(audio)
        console.keyManager = keyManager

        console.notificationManager.registerNotificationListener(this)
    }

    override fun close() {
        console.close()

        emuThread.getAndSet(null)?.cancel(true)
    }

    override fun processNotification(type: NotificationType, vararg data: Any?) = Unit

    fun load(rom: ByteArray, name: String, fdsBios: ByteArray = ByteArray(0)): Boolean {
        return if (console.initialize(rom, name, true, fdsBios)) {
            emuThread.compareAndSet(null, threadExecutor.submit(console))

            for (provider in inputProviders) {
                console.controlManager.registerInputProvider(provider)
            }

            while (!console.isRunning) Thread.sleep(10)

            true
        } else {
            false
        }
    }

    fun pause() {
        if (running) {
            settings.flag(EmulationFlag.PAUSED, true)
        }
    }

    val paused
        get() = running && settings.flag(EmulationFlag.PAUSED)

    fun resume() {
        if (running) {
            settings.flag(EmulationFlag.PAUSED, false)
        }
    }

    fun stop() {
        if (!running) return

        console.stop()
        emuThread.getAndSet(null)?.cancel(true)
    }

    override fun reset(softReset: Boolean) {
        if (!running) return

        if (console.debugger.isExecutionStopped) {
            console.resetComponents(true)
            console.controlManager.updateInputState()
            console.debugger.cpuStep(1)
        } else {
            console.reset(true)
        }
    }

    fun powerCycle() {
        if (!running) return

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

    fun takeScreenshot(): BufferedImage? {
        if (!console.canScreenshot) return null
        val image = BufferedImage(Ppu.SCREEN_WIDTH, Ppu.SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB)
        takeScreenshot(image)
        return image
    }

    fun takeScreenshot(image: BufferedImage): Boolean {
        if (!console.canScreenshot) return false
        val data = (image.raster.dataBuffer as DataBufferInt).data
        console.takeScreenshot().copyInto(data)
        return true
    }

    fun cheats(cheats: Iterable<CheatInfo>) {
        console.cheatManager.set(cheats)
    }

    fun saveState(): ByteArray {
        val snapshot = Snapshot()
        saveState(snapshot)
        return snapshot.bytes()
    }

    fun saveState(sink: OutputStream) {
        val snapshot = Snapshot()
        saveState(snapshot)
        snapshot.writeTo(sink)
    }

    fun saveState(snapshot: Snapshot) {
        if (!running) return

        try {
            console.pause()
            console.saveStateManager.saveState(snapshot)
        } finally {
            console.resume()
        }
    }

    fun restoreState(data: ByteArray) {
        restoreState(Snapshot.from(data))
    }

    fun restoreState(source: InputStream) {
        restoreState(Snapshot.from(source))
    }

    fun restoreState(snapshot: Snapshot) {
        if (!running) return

        try {
            console.pause()
            console.saveStateManager.restoreState(snapshot)
        } finally {
            console.resume()
        }
    }

    val isFds
        get() = console.isFds

    val isNsf
        get() = console.isNsf

    val isVsSystem
        get() = console.isVsSystem

    // TODO: Mover pro Console
    // TODO: Remover a classe FdsSystemActionManager e substituir por FdsInputButtons
    val fdsSystemActionManager
        get() = if (!console.isRunning) null else console.systemActionManager as? FdsSystemActionManager

    @Suppress("UNCHECKED_CAST")
    fun <T : ControlDevice> controlDevice(port: Int): T? {
        return console.controlManager.controlDevice(port) as? T
    }

    val fdsSideCount
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

    fun insertCoin(port: Int) {
        console.insertCoin(port)
    }

    inline val dipSwitchCount
        get() = console.dipSwitchCount

    inline val info
        get() = console.mapper!!.info

    fun debugRunFrame(count: Int = 1) {
        if (running) {
            console.debugger.frameStep(count)
        }
    }

    fun debugRunScanline(count: Int = 1) {
        if (running) {
            console.debugger.scanlineStep(count)
        }
    }

    fun debugRunPpuCycle(count: Int = 1) {
        if (running) {
            console.debugger.ppuStep(count)
        }
    }

    fun debugRunCpuCycle(count: Int = 1) {
        if (running) {
            console.debugger.cpuStep(count)
        }
    }

    fun debugRun() {
        if (running) {
            console.debugger.run()
        }
    }

    fun debugStep(count: Int) {
        if (running) {
            console.debugger.step(count)
        }
    }
}
