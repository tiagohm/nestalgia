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
    private val threadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    val emulator = Emulator(console, Speaker, Video(), controller, emptyList(), threadExecutor)

    var isAutoPlugController = true
        internal set

    fun start() {
        if (isAutoPlugController && emulator.settings.port1.type == NONE) {
            emulator.settings.port1.type = NES_CONTROLLER
            CONTROLLER_KEYS_1P.copyTo(emulator.settings.port1.keyMapping)
        }

        emulator.load(path.readBytes().toIntArray(), path.nameWithoutExtension)
    }

    fun softReset() {
        emulator.reset(true)
    }

    fun stop() {
        emulator.stop()
    }

    fun press(button: ControllerButton, port: Int = 0) {
        val keys = CONTROLLER_KEYS[port]
        pressedButtons[keys.key(button).code] = 1
    }

    fun pressAndRelease(button: ControllerButton, port: Int = 0) {
        val keys = CONTROLLER_KEYS[port]
        pressedButtons[keys.key(button).code] = 2
    }

    fun release(button: ControllerButton, port: Int = 0) {
        val keys = CONTROLLER_KEYS[port]
        pressedButtons[keys.key(button).code] = 0
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

        @JvmStatic internal val CONTROLLER_KEYS_1P = KeyMapping(A, B, C, D, E, F, G, H)
        @JvmStatic internal val CONTROLLER_KEYS_2P = KeyMapping(I, J, K, L, M, N, O, P)
        @JvmStatic internal val CONTROLLER_KEYS_3P = KeyMapping(Q, R, S, T, U, V, W, X)
        @JvmStatic internal val CONTROLLER_KEYS_4P = KeyMapping(Y, Z, F1, F2, F3, F4, F5, F6)

        @JvmStatic internal val CONTROLLER_KEYS = arrayOf(
            CONTROLLER_KEYS_1P, CONTROLLER_KEYS_2P, CONTROLLER_KEYS_3P, CONTROLLER_KEYS_4P,
        )
    }
}
