package br.tiagohm.nestalgia.ui

import br.tiagohm.nestalgia.core.Console
import br.tiagohm.nestalgia.core.Disposable
import br.tiagohm.nestalgia.core.NotificationListener
import br.tiagohm.nestalgia.core.NotificationType
import java.awt.Color
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.JLabel
import javax.swing.JPanel

class StatusBar(val console: Console) :
    JPanel(),
    Disposable,
    NotificationListener {

    enum class Type(val color: Color) {
        INFO(Color.BLUE),
        SUCCESS(Color.GREEN),
        WARNING(Color.ORANGE),
        ERROR(Color.RED),
    }

    private val text = JLabel(DEBUG_TEXT).also { it.font = it.font.deriveFont(FONT_SIZE) }
    private val executor = Executors.newScheduledThreadPool(1)
    private var handler: Future<*>? = null

    init {
        add(text)

        console.notificationManager.registerNotificationListener(this)

        isOpaque = true
        isVisible = false
    }

    override fun dispose() {
        console.notificationManager.unregisterNotificationListener(this)
        executor.shutdownNow()
        handler = null
    }

    override fun processNotification(type: NotificationType, vararg data: Any?) {
        when (type) {
            NotificationType.DEBUG_CONTINUE -> isVisible = console.debugger.isExecutionStopped
            NotificationType.DEBUG_BREAK -> {
                showDebugInfo()
                isVisible = true
            }
        }
    }

    fun showText(text: String, timeout: Long = 5000, type: Type = Type.INFO) {
        this.text.text = text
        isVisible = true
        background = type.color

        handler?.cancel(true)

        if (timeout > 0) {
            handler = executor.schedule({
                if (console.mapper != null && console.debugger.isExecutionStopped) {
                    showDebugInfo()
                } else {
                    isVisible = false
                }

                handler = null
            }, timeout, TimeUnit.MILLISECONDS)
        }
    }

    fun hideText() {
        handler?.cancel(true)
        handler = null
        isVisible = false
    }

    private fun showDebugInfo() {
        if (console.isRunning) {
            val text = String.format(
                DEBUG_TEXT,
                console.cpu.a.toInt(),
                console.cpu.x.toInt(),
                console.cpu.y.toInt(),
                console.cpu.pc.toInt(),
                console.cpu.ps.toInt(),
                console.cpu.sp.toInt(),
                console.cpu.cycleCount.toInt(),
                console.ppu.cycle,
                console.ppu.scanline,
                console.ppu.frameCount,
            )

            showText(text, 0)
        }
    }

    companion object {
        private const val FONT_SIZE = 10f
        private const val DEBUG_TEXT =
            "A: %02X X: %02X Y: %02X PC: %04X PS: %02X SP: %02X CYCLE: %d PPU CYCLE: %d SCANLINE: %d FRAME: %d"
    }
}