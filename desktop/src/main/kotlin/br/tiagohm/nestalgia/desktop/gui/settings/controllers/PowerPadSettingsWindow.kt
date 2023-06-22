package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class PowerPadSettingsWindow(
    private val keyMapping: KeyMapping,
    private val type: ControllerType,
) : AbstractWindow() {

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

    private lateinit var buttonComboBoxes: Array<ComboBox<Key>>

    override fun onCreate() {
        title = if (type == POWER_PAD_SIDE_A || type == POWER_PAD_SIDE_B) "Power Pad" else "Family Trainer Mat"
        resizable = false

        buttonComboBoxes = arrayOf(
            button01ComboBox, button02ComboBox, button03ComboBox, button04ComboBox,
            button05ComboBox, button06ComboBox, button07ComboBox, button08ComboBox,
            button09ComboBox, button10ComboBox, button11ComboBox, button12ComboBox,
        )

        buttonComboBoxes.forEach { it.converter = KeyStringConverter }
        buttonComboBoxes.forEach { it.items.setAll(KeyboardKeys.SORTED_KEYS) }
    }

    override fun onStart() {
        for (button in PowerPadButton.entries) {
            buttonComboBoxes[button.ordinal].value = keyMapping.customKey(button)
        }
    }

    override fun onStop() {
        for (button in PowerPadButton.entries) {
            keyMapping.customKey(button, buttonComboBoxes[button.ordinal].value)
        }
    }

    @FXML
    private fun clearKeyBindings() {
        buttonComboBoxes.forEach { it.value = Key.UNDEFINED }
    }
}
