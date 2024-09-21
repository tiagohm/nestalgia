package br.tiagohm.nestalgia.desktop.video

import br.tiagohm.nestalgia.core.Ppu.Companion.SCREEN_HEIGHT
import br.tiagohm.nestalgia.core.Ppu.Companion.SCREEN_WIDTH
import br.tiagohm.nestalgia.core.RenderingDevice
import br.tiagohm.nestalgia.desktop.gui.CanvasPane
import javafx.scene.CacheHint
import javafx.scene.image.RenderedImage

class Television : CanvasPane(SCREEN_WIDTH, SCREEN_HEIGHT), RenderingDevice {

    private val data = IntArray(SCREEN_WIDTH * SCREEN_HEIGHT)
    private val image = RenderedImage(data, SCREEN_WIDTH, SCREEN_HEIGHT)

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
        with(canvas.graphicsContext2D) {
            image.render()
            drawImage(image, 0.0, 0.0, width, height)
        }
    }

    override fun reset(softReset: Boolean) = Unit

    override fun close() = Unit
}
