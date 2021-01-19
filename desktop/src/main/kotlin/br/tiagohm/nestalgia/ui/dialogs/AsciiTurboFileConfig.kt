package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.ui.*
import javax.swing.JPanel

@ExperimentalUnsignedTypes
class AsciiTurboFileConfig(
    private var slot: Int,
    private val onOk: (Int) -> Unit,
) : Dialog("Ascii Turbo File", minWidth = 300) {

    override val body: JPanel
        get() {
            return panel(2, 3, margin = margin(16)) {
                label("Slot", 0, 0)
                dropdown(
                    0, 2,
                    listOf(0, 1, 2, 3),
                    slot,
                    onChanged = {
                        slot = it
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
        onOk(slot)
        dispose()
    }

    companion object {
        fun show(slot: Int, onOk: (Int) -> Unit) {
            val dialog = AsciiTurboFileConfig(slot, onOk)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}