package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.PartyTap
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class PartyTapSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow<PartyTap.Button>() {

    override val resourceName = "PartyTapSettings"

    @FXML private lateinit var b1ComboBox: ComboBox<Key>
    @FXML private lateinit var b2ComboBox: ComboBox<Key>
    @FXML private lateinit var b3ComboBox: ComboBox<Key>
    @FXML private lateinit var b4ComboBox: ComboBox<Key>
    @FXML private lateinit var b5ComboBox: ComboBox<Key>
    @FXML private lateinit var b6ComboBox: ComboBox<Key>

    override lateinit var buttonComboBoxes: Array<ComboBox<Key>?>
    override val buttonEntries = PartyTap.Button.entries

    override fun onCreate() {
        title = "Party Tap"

        buttonComboBoxes = arrayOf(b1ComboBox, b2ComboBox, b3ComboBox, b4ComboBox, b5ComboBox, b6ComboBox)

        super.onCreate()
    }
}
