package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerType
import br.tiagohm.nestalgia.core.ControllerType.POWER_PAD_SIDE_A
import br.tiagohm.nestalgia.core.ControllerType.POWER_PAD_SIDE_B
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.PowerPad
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class PowerPadSettingsWindow(
    override val keyMapping: KeyMapping,
    private val type: ControllerType,
) : AbstractControllerWindow<PowerPad.Button>() {

    override val resourceName = "PowerPadSettings"

    @FXML private lateinit var button01ComboBox: ComboBox<Key>
    @FXML private lateinit var button02ComboBox: ComboBox<Key>
    @FXML private lateinit var button03ComboBox: ComboBox<Key>
    @FXML private lateinit var button04ComboBox: ComboBox<Key>
    @FXML private lateinit var button05ComboBox: ComboBox<Key>
    @FXML private lateinit var button06ComboBox: ComboBox<Key>
    @FXML private lateinit var button07ComboBox: ComboBox<Key>
    @FXML private lateinit var button08ComboBox: ComboBox<Key>
    @FXML private lateinit var button09ComboBox: ComboBox<Key>
    @FXML private lateinit var button10ComboBox: ComboBox<Key>
    @FXML private lateinit var button11ComboBox: ComboBox<Key>
    @FXML private lateinit var button12ComboBox: ComboBox<Key>

    override lateinit var buttonComboBoxes: Array<ComboBox<Key>?>
    override val buttonEntries = PowerPad.Button.entries

    override fun onCreate() {
        title = if (type == POWER_PAD_SIDE_A || type == POWER_PAD_SIDE_B) "Power Pad" else "Family Trainer Mat"

        buttonComboBoxes = arrayOf(
            button01ComboBox, button02ComboBox, button03ComboBox, button04ComboBox,
            button05ComboBox, button06ComboBox, button07ComboBox, button08ComboBox,
            button09ComboBox, button10ComboBox, button11ComboBox, button12ComboBox,
        )

        super.onCreate()
    }
}
