package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ExcitingBoxingButton
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class ExcitingBoxingSettingsWindow(private val keyMapping: KeyMapping) : AbstractWindow() {

    override val resourceName = "ExcitingBoxingSettings"

    @FXML private lateinit var bodyComboBox: ComboBox<Key>
    @FXML private lateinit var hookLeftComboBox: ComboBox<Key>
    @FXML private lateinit var hookRightComboBox: ComboBox<Key>
    @FXML private lateinit var jabLeftComboBox: ComboBox<Key>
    @FXML private lateinit var jabRightComboBox: ComboBox<Key>
    @FXML private lateinit var moveLeftComboBox: ComboBox<Key>
    @FXML private lateinit var moveRightComboBox: ComboBox<Key>
    @FXML private lateinit var straightComboBox: ComboBox<Key>

    private lateinit var buttonComboBoxes: Array<ComboBox<Key>>

    override fun onCreate() {
        title = "Exciting Boxing Punching Bag"
        resizable = false

        buttonComboBoxes = arrayOf(
            bodyComboBox, hookLeftComboBox, hookRightComboBox, jabLeftComboBox,
            jabRightComboBox, moveLeftComboBox, moveRightComboBox, straightComboBox,
        )

        buttonComboBoxes.forEach { it.converter = KeyStringConverter }
        buttonComboBoxes.forEach { it.items.setAll(KeyboardKeys.SORTED_KEYS) }
    }

    override fun onStart() {
        for (button in ExcitingBoxingButton.values()) {
            buttonComboBoxes[button.ordinal].value = keyMapping.customKey(button)
        }
    }

    override fun onStop() {
        for (button in ExcitingBoxingButton.values()) {
            keyMapping.customKey(button, buttonComboBoxes[button.ordinal].value)
        }
    }

    @FXML
    private fun clearKeyBindings() {
        buttonComboBoxes.forEach { it.value = Key.UNDEFINED }
    }
}
