import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.KeyboardKeys.*
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Executors
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readBytes

class NesTester(private val path: Path) {

    private val pressedButtons = IntArray(256)

    val console = Console()
    val controller: KeyManager = Controller()
    @JvmField internal val frameHashes = Collections.synchronizedSet(HashSet<String>(2048))
    private val threadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), EmulatorThreadFactory)
    val emulator = Emulator(console, Speaker, Video(), controller, emptyList(), threadExecutor)

    fun start() {
        emulator.load(path.readBytes().toIntArray(), path.nameWithoutExtension)
    }

    fun softReset() {
        emulator.reset(true)
    }

    fun stop() {
        emulator.stop()
    }

    private fun controllerKeys(button: ControllerButton, port: Int): KeyMapping {
        return when (button) {
            is PowerPadButton -> POWER_PAD_KEYS
            else -> CONTROLLER_KEYS[port]
        }
    }

    fun press(button: ControllerButton, port: Int = 0) {
        val keys = controllerKeys(button, port)
        pressedButtons[keys.key(button).code] = 1
    }

    fun pressAndRelease(button: ControllerButton, port: Int = 0) {
        val keys = controllerKeys(button, port)
        pressedButtons[keys.key(button).code] = 2
    }

    fun release(button: ControllerButton, port: Int = 0) {
        val keys = controllerKeys(button, port)
        pressedButtons[keys.key(button).code] = 0
    }

    fun ControllerSettings.configureNoControllerForThisPort() {
        type = NONE
        keyMapping.reset()
    }

    fun ControllerSettings.configureStandardControllerForThisPort() {
        type = NES_CONTROLLER

        val port = if (this === console.settings.port1) 0
        else if (this === console.settings.port2) 1
        else 0

        CONTROLLER_KEYS[port].copyTo(keyMapping)
    }

    fun ControllerSettings.configureFourScoreForThisPort() {
        if (this === console.settings.port1) {
            type = FOUR_SCORE

            repeat(4) { console.settings.subPort1[it].type = NES_CONTROLLER }
            repeat(4) { CONTROLLER_KEYS[it].copyTo(console.settings.subPort1[it].keyMapping) }
        }
    }

    fun ControllerSettings.configureZapperForThisPort() {
        type = if (this === console.settings.expansionPort) FAMICOM_ZAPPER else NES_ZAPPER
    }

    fun ControllerSettings.configureArkanoidForThisPort() {
        type = if (this === console.settings.expansionPort) FAMICOM_ARKANOID_CONTROLLER else NES_ARKANOID_CONTROLLER
    }

    fun ControllerSettings.configurePowerPadForThisPort() {
        type = POWER_PAD_SIDE_A
        POWER_PAD_KEYS.copyTo(keyMapping)
    }

    private object Speaker : AudioDevice {

        override fun play(buffer: ShortArray, length: Int, sampleRate: Int, stereo: Boolean) {}

        override fun stop() {}

        override fun pause() {}

        override fun processEndOfFrame() {}

        override fun close() {}
    }

    private inner class Video : RenderingDevice {

        override fun updateFrame(buffer: IntArray, width: Int, height: Int) {
            frameHashes.add(buffer.md5())
        }

        override fun render() {}

        override fun reset(softReset: Boolean) {}

        override fun close() {}
    }

    private inner class Controller : KeyManager {

        override fun isKeyPressed(key: Key): Boolean {
            val pressed = pressedButtons[key.code]
            if (pressed == 2) pressedButtons[key.code] = 0
            return pressed >= 1
        }

        override fun refreshKeyState() {}

        override var mouseX = 0
            private set

        override var mouseY = 0
            private set
    }

    companion object {

        @JvmStatic private val CONTROLLER_KEYS_1P = KeyMapping(A, B, C, D, E, F, G, H)
        @JvmStatic private val CONTROLLER_KEYS_2P = KeyMapping(I, J, K, L, M, N, O, P)
        @JvmStatic private val CONTROLLER_KEYS_3P = KeyMapping(Q, R, S, T, U, V, W, X)
        @JvmStatic private val CONTROLLER_KEYS_4P = KeyMapping(Y, Z, F1, F2, F3, F4, F5, F6)

        @JvmStatic private val CONTROLLER_KEYS = arrayOf(CONTROLLER_KEYS_1P, CONTROLLER_KEYS_2P, CONTROLLER_KEYS_3P, CONTROLLER_KEYS_4P)

        @JvmStatic private val POWER_PAD_CUSTOM_KEYS = arrayOf<Key>(I, J, K, L, M, N, O, P, Q, R, S, T)
        @JvmStatic private val POWER_PAD_KEYS = KeyMapping(A, B, C, D, E, F, G, H, customKeys = POWER_PAD_CUSTOM_KEYS)
    }
}
