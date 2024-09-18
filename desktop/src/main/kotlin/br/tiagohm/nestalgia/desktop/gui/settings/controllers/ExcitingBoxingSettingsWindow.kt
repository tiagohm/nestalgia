package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ExcitingBoxingController
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class ExcitingBoxingSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow<ExcitingBoxingController.Button>() {

    override val resourceName = "ExcitingBoxingSettings"

    @FXML private lateinit var bodyComboBox: ComboBox<Key>
    @FXML private lateinit var hookLeftComboBox: ComboBox<Key>
    @FXML private lateinit var hookRightComboBox: ComboBox<Key>
    @FXML private lateinit var jabLeftComboBox: ComboBox<Key>
    @FXML private lateinit var jabRightComboBox: ComboBox<Key>
    @FXML private lateinit var moveLeftComboBox: ComboBox<Key>
    @FXML private lateinit var moveRightComboBox: ComboBox<Key>
    @FXML private lateinit var straightComboBox: ComboBox<Key>

    override lateinit var buttonComboBoxes: Array<ComboBox<Key>?>
    override val buttonEntries = ExcitingBoxingController.Button.entries

    override fun onCreate() {
        title = "Exciting Boxing Punching Bag"

        buttonComboBoxes = arrayOf(
            bodyComboBox, hookLeftComboBox, hookRightComboBox, jabLeftComboBox,
            jabRightComboBox, moveLeftComboBox, moveRightComboBox, straightComboBox,
        )

        super.onCreate()
    }
}
