package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.core.KeyboardKeys.*
import br.tiagohm.nestalgia.core.PowerPadButton
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class PowerPadSettingsWindow(private val keyMapping: KeyMapping) : AbstractWindow() {

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
        title = "Power Pad"
        resizable = false

        buttonComboBoxes = arrayOf(
            button01ComboBox, button02ComboBox, button03ComboBox, button04ComboBox,
            button05ComboBox, button06ComboBox, button07ComboBox, button08ComboBox,
            button09ComboBox, button10ComboBox, button11ComboBox, button12ComboBox,
        )

        with(KeyboardKeys.SORTED_KEYS) {
            buttonComboBoxes.forEach { it.items.setAll(this) }
        }
    }

    override fun onStart() {
        for (button in PowerPadButton.values()) {
            buttonComboBoxes[button.ordinal].value = keyMapping.customKey(button)
        }
    }

    override fun onStop() {
        for (button in PowerPadButton.values()) {
            keyMapping.customKey(button, buttonComboBoxes[button.ordinal].value)
        }
    }

    @FXML
    private fun useDefaultBinding() {
        for (i in DEFAULT_KEYS.indices) {
            buttonComboBoxes[i].value = DEFAULT_KEYS[i]
        }
    }

    @FXML
    private fun clearKeyBindings() {
        buttonComboBoxes.forEach { it.value = Key.UNDEFINED }
    }

    companion object {

        @JvmStatic private val DEFAULT_KEYS = arrayOf(R, T, Y, U, F, G, H, J, C, V, B, N)
    }
}
