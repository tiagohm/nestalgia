import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.ControllerType.*
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readBytes

class NesTester {

    private val console = Console()
    private val speaker = Speaker()
    private val video = Video()
    private val controller = Controller()
    private val emulator = Emulator(console, speaker, video, controller, emptyList())
    private val defaultKeyMapping = KeyMapping(7, 6, 0, 1, 2, 3, 4, 5, 8)
    private val pressedButtons = BooleanArray(64)

    internal val frameHashes = HashSet<String>(2048)

    fun load(path: Path) {
        frameHashes.clear()
        emulator.settings.controllerKeys(0, defaultKeyMapping)
        emulator.settings.controllerType(0, NES_CONTROLLER)
        emulator.load(path.readBytes().toIntArray(), path.nameWithoutExtension)
    }

    var region
        get() = emulator.settings.region
        set(value) {
            emulator.settings.region = value
        }

    fun softReset() {
        emulator.reset(true)
    }

    fun stop() {
        emulator.stop()
    }

    fun pressButton(button: StandardControllerButton) {
        pressedButtons[button.bit] = true
    }

    fun takeScreenshot(): IntArray {
        return console.takeScreenshot()
    }

    fun takeScreenshotAsImage(): BufferedImage? {
        return emulator.takeScreenshot()
    }

    private inner class Speaker : AudioDevice {

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

        override fun isKeyPressed(keyCode: Int): Boolean {
            val pressed = pressedButtons[keyCode]
            pressedButtons[keyCode] = false
            return pressed
        }

        override fun isMouseButtonPressed(mouseButton: MouseButton) = false

        override fun refreshKeyState() {}

        override var mouseX = 0
            private set

        override var mouseY = 0
            private set
    }
}
