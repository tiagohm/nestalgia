package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.core.BreakOnType
import br.tiagohm.nestalgia.ui.*
import javax.swing.JPanel
import kotlin.math.max

@ExperimentalUnsignedTypes
class BreakOnConfig(
    private var type: BreakOnType,
    private var count: Int,
    private val onOk: (BreakOnType, Int) -> Unit,
) : Dialog("Break On") {

    private val min: Int
        get() = if (type == BreakOnType.SCANLINE) -1 else 0

    override val body: JPanel
        get() {
            return panel(3, 3, margin = margin(16)) {
                label("Type: ", 0, 0)
                dropdown(
                    0, 1,
                    BREAK_ON_TYPES,
                    type,
                    colSpan = 2,
                    onChanged = {
                        type = it
                        count = max(count, min)
                        updateView()
                    })

                label("Count: ", 1, 0)
                spinnerNumber(
                    1, 1,
                    count, min, Int.MAX_VALUE,
                    colSpan = 2,
                    isEnabled = type != BreakOnType.NONE,
                    onChanged = {
                        count = it
                    })

                button(
                    "OK",
                    2, 0,
                    colSpan = 3,
                    fill = Fill.HORIZONTAL,
                    anchor = Anchor.WEST,
                    onClick = ::onOk
                )
            }
        }

    private fun onOk() {
        onOk(type, count)
        dispose()
    }

    companion object {
        private val BREAK_ON_TYPES = BreakOnType.values().toList()

        fun show(type: BreakOnType, count: Int, onOk: (BreakOnType, Int) -> Unit) {
            val dialog = BreakOnConfig(type, count, onOk)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}