package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.BandaiMicrophone
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

open class BandaiMicrophoneSettingsWindow(protected val keyMapping: KeyMapping) : AbstractWindow() {

    override val resourceName = "BandaiMicrophoneSettings"

    @FXML protected lateinit var aComboBox: ComboBox<Key>
    @FXML protected lateinit var bComboBox: ComboBox<Key>
    @FXML protected lateinit var microphoneComboBox: ComboBox<Key>

    protected lateinit var buttonComboBoxes: Array<ComboBox<Key>>

    override fun onCreate() {
        title = "Bandai Microphone"
        resizable = false

        buttonComboBoxes = arrayOf(aComboBox, bComboBox, microphoneComboBox)

        buttonComboBoxes.forEach { it.converter = KeyStringConverter }
        buttonComboBoxes.forEach { it.items.setAll(KeyboardKeys.SORTED_KEYS) }
    }

    override fun onStart() {
        for (button in BandaiMicrophone.Button.entries) {
            buttonComboBoxes[button.ordinal].value = keyMapping.customKey(button)
        }
    }

    override fun onStop() {
        for (button in BandaiMicrophone.Button.entries) {
            keyMapping.customKey(button, buttonComboBoxes[button.ordinal].value)
        }
    }

    @FXML
    private fun clearKeyBindings() {
        buttonComboBoxes.forEach { it.value = Key.UNDEFINED }
    }
}
