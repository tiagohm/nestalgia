package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.ui.*
import javax.swing.JPanel

class ZapperConfig(
    port: Int,
    private var radius: Int,
    private val onOk: (Int) -> Unit,
) : Dialog("Zapper Settings - Port $port", minWidth = 300) {

    override val body: JPanel
        get() {
            return panel(2, 3, margin = margin(16)) {
                label("Light Detection Radius", 0, 0)
                dropdown(
                    0, 2,
                    listOf(0, 1, 2, 3),
                    radius,
                    onChanged = {
                        radius = it
                    })

                button(
                    "OK",
                    1, 0,
                    colSpan = 3,
                    fill = Fill.HORIZONTAL,
                    anchor = Anchor.WEST,
                    onClick = ::onOk
                )
            }
        }

    private fun onOk() {
        onOk(radius)
        dispose()
    }

    companion object {
        fun show(port: Int, radius: Int, onOk: (Int) -> Unit) {
            val dialog = ZapperConfig(port, radius, onOk)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}