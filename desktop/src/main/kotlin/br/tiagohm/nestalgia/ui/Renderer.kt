package br.tiagohm.nestalgia.ui

import br.tiagohm.nestalgia.core.Console
import br.tiagohm.nestalgia.core.Ppu
import br.tiagohm.nestalgia.core.RenderingDevice
import java.awt.Canvas
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.image.BufferStrategy
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Renderer(val console: Console) :
    Canvas(),
    ComponentListener,
    RenderingDevice {

    private val image = BufferedImage(256, 240, BufferedImage.TYPE_INT_ARGB)
    private var buffer: BufferStrategy? = null
    private var graphics: Graphics2D? = null

    init {
        setSize(Ppu.SCREEN_WIDTH * 2, Ppu.SCREEN_HEIGHT * 2)
        addComponentListener(this)
        ignoreRepaint = true
    }

    override val screenWidth: Int
        get() = width

    override val screenHeight: Int
        get() = height

    fun initialize() {
        createBufferStrategy(2)
        buffer = bufferStrategy
        initializeGraphics()
    }

    private inline fun initializeGraphics() {
        graphics = buffer?.drawGraphics as Graphics2D
        graphics?.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
        graphics?.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED)
        graphics?.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
    }

    override fun dispose() {
        graphics?.dispose()
        removeComponentListener(this)
    }

    override fun updateFrame(buffer: IntArray, width: Int, height: Int) {
        val data = (image.raster.dataBuffer as DataBufferInt).data
        buffer.copyInto(data)
    }

    override fun render() {
        buffer?.let {
            synchronized(it) {
                graphics?.drawImage(image, 0, 0, width, height, null)
                it.show()
            }
        }
    }

    override fun reset(softReset: Boolean) {}

    override fun componentResized(e: ComponentEvent) {
        buffer?.let {
            synchronized(it) {
                graphics?.dispose()
                initializeGraphics()
            }
        }
    }

    override fun componentMoved(e: ComponentEvent) {}

    override fun componentShown(e: ComponentEvent) {}

    override fun componentHidden(e: ComponentEvent) {}
}