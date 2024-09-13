package br.tiagohm.nestalgia.desktop.gui.cheats

import br.tiagohm.nestalgia.core.CheatDatabase
import br.tiagohm.nestalgia.core.CheatInfo
import br.tiagohm.nestalgia.desktop.console
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

class CheatsWindow : AbstractWindow() {

    override val resourceName = "Cheats"

    @FXML private lateinit var cheatsListView: ListView<CheatInfo>

    val selectedCheats = hashSetOf<CheatInfo>()

    var saved = false
        private set

    override fun onCreate() {
        title = "Cheats"
        resizable = false

        cheatsListView.cellFactory = CheatInfoCellFactory()
    }

    override fun onStart() {
        super.onStart()

        val info = console.mapper?.info
        val id = info?.hash?.prgCrc32 ?: 0L
        val games = if (id != 0L) CheatDatabase[id] else emptyList()

        cheatsListView.items.setAll(games)

        for (game in games) {
            if (game !in selectedCheats) {
                selectedCheats.remove(game)
                saved = true
            }
        }
    }

    private inner class CheatInfoCellFactory : Callback<ListView<CheatInfo>, ListCell<CheatInfo>> {

        override fun call(param: ListView<CheatInfo>): ListCell<CheatInfo> {
            return CheatInfoListCell()
        }
    }

    private inner class CheatInfoListCell : ListCell<CheatInfo>() {

        override fun updateItem(item: CheatInfo?, empty: Boolean) {
            super.updateItem(item, empty)

            if (empty || item == null) {
                text = null
                graphic = null
            } else {
                val box = CheckBox(item.description)
                box.isSelected = item in selectedCheats
                box.selectedProperty().addListener { _, _, value ->
                    if (value) selectedCheats.add(item) else selectedCheats.remove(item)
                    saved = true
                }
                text = null
                graphic = box
            }
        }
    }
}
