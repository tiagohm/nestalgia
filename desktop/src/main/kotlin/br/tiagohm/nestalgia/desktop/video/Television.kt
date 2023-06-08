package br.tiagohm.nestalgia.desktop.video

import br.tiagohm.nestalgia.core.Ppu
import br.tiagohm.nestalgia.core.RenderingDevice
import br.tiagohm.nestalgia.desktop.gui.CanvasPane
import javafx.scene.CacheHint
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import java.nio.IntBuffer

class Television : CanvasPane(Ppu.SCREEN_WIDTH, Ppu.SCREEN_HEIGHT), RenderingDevice {

    private val data = IntArray(Ppu.SCREEN_WIDTH * Ppu.SCREEN_HEIGHT)
    private val buffer = IntBuffer.wrap(data)
    private val pixelBuffer = PixelBuffer(Ppu.SCREEN_WIDTH, Ppu.SCREEN_HEIGHT, buffer, PixelFormat.getIntArgbPreInstance())

    init {
        canvas.isCache = false
        canvas.cacheHint = CacheHint.SPEED
        canvas.graphicsContext2D.isImageSmoothing = false
    }

    override fun isResizable() = true

    override fun updateFrame(buffer: IntArray, width: Int, height: Int) {
        buffer.copyInto(data)
    }

    override fun render() {
        // buffer.clear()

        with(canvas.graphicsContext2D) {
            // clearRect(0.0, 0.0, width, height)
            drawImage(WritableImage(pixelBuffer), 0.0, 0.0, width, height)
        }
    }

    override fun reset(softReset: Boolean) {}

    override fun close() {}
}
