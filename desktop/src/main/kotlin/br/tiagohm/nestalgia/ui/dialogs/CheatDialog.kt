package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.core.CheatDatabase
import br.tiagohm.nestalgia.core.CheatInfo
import br.tiagohm.nestalgia.ui.Anchor
import br.tiagohm.nestalgia.ui.Fill
import br.tiagohm.nestalgia.ui.SelectionMode
import br.tiagohm.nestalgia.ui.Size
import br.tiagohm.nestalgia.ui.button
import br.tiagohm.nestalgia.ui.checkboxList
import br.tiagohm.nestalgia.ui.label
import br.tiagohm.nestalgia.ui.margin
import br.tiagohm.nestalgia.ui.panel
import br.tiagohm.nestalgia.ui.scrollPane
import javax.swing.JPanel

class CheatDialog(
    private val game: Long,
    private val selectedCheats: MutableList<CheatInfo>,
    private val onSave: () -> Unit,
) : Dialog("Cheats") {

    private val items = ArrayList(selectedCheats)

    override val body: JPanel
        get() {
            val cheats = CheatDatabase.getByGame(game)

            return panel(2, 3, margin = margin(if (cheats.isEmpty()) 128 else 16)) {
                if (cheats.isNotEmpty()) {
                    scrollPane(
                        0, 0,
                        colSpan = 3,
                    ) {
                        checkboxList(
                            0, 0,
                            cheats,
                            items,
                            selectionMode = SelectionMode.MULTIPLE,
                            onChanged = { checked: Boolean, _: Int, cheat: CheatInfo ->
                                if (checked) {
                                    items.add(cheat)
                                } else {
                                    items.remove(cheat)
                                }
                            }
                        )
                    }

                    button(
                        "OK",
                        1, 1,
                        fill = Fill.NONE,
                        anchor = Anchor.CENTER,
                        minimumSize = Size(100),
                        onClick = ::onOk
                    )
                } else {
                    label("No available cheats", 0, 0, colSpan = 3)
                }
            }
        }

    private fun onOk() {
        selectedCheats.clear()
        selectedCheats.addAll(items)
        onSave()
        dispose()
    }

    companion object {
        fun show(game: Long, selectedCheats: MutableList<CheatInfo>, onSave: () -> Unit) {
            val dialog = CheatDialog(game, selectedCheats, onSave)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}
