package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KonamiHyperShot
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class KonamiHyperShotSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow<KonamiHyperShot.Button>() {

    override val resourceName = "KonamiHyperShotSettings"

    @FXML private lateinit var jumpP1ComboBox: ComboBox<Key>
    @FXML private lateinit var runP1ComboBox: ComboBox<Key>
    @FXML private lateinit var jumpP2ComboBox: ComboBox<Key>
    @FXML private lateinit var runP2ComboBox: ComboBox<Key>

    override lateinit var buttonComboBoxes: Array<ComboBox<Key>?>
    override val buttonEntries = KonamiHyperShot.Button.entries

    override fun onCreate() {
        title = "Konami Hyper Shot"

        buttonComboBoxes = arrayOf(jumpP1ComboBox, runP1ComboBox, jumpP2ComboBox, runP2ComboBox)

        super.onCreate()
    }
}
