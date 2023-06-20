package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.core.StandardControllerButton
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

data class StandardControllerSettingsWindow(private val keyMapping: KeyMapping) : AbstractWindow() {

    override val resourceName = "StandardControllerSettings"

    @FXML private lateinit var upComboBox: ComboBox<Key>
    @FXML private lateinit var downComboBox: ComboBox<Key>
    @FXML private lateinit var leftComboBox: ComboBox<Key>
    @FXML private lateinit var rightComboBox: ComboBox<Key>
    @FXML private lateinit var startComboBox: ComboBox<Key>
    @FXML private lateinit var selectComboBox: ComboBox<Key>
    @FXML private lateinit var bComboBox: ComboBox<Key>
    @FXML private lateinit var aComboBox: ComboBox<Key>
    @FXML private lateinit var microphoneComboBox: ComboBox<Key>
    @FXML private lateinit var turboBComboBox: ComboBox<Key>
    @FXML private lateinit var turboAComboBox: ComboBox<Key>
    @FXML private lateinit var presetComboBox: ComboBox<String>

    private lateinit var buttonComboBoxes: Array<ComboBox<Key>>

    override fun onCreate() {
        title = "NES/Famicom Controller"
        resizable = false

        buttonComboBoxes = arrayOf(
            upComboBox, downComboBox, leftComboBox, rightComboBox,
            startComboBox, selectComboBox, bComboBox, aComboBox,
            microphoneComboBox, turboBComboBox, turboAComboBox,
        )

        buttonComboBoxes.forEach { it.converter = KeyStringConverter }
        buttonComboBoxes.forEach { it.items.setAll(KeyboardKeys.SORTED_KEYS) }

        apply(keyMapping)
    }

    override fun onStop() {
        for (button in StandardControllerButton.values()) {
            keyMapping.key(button, buttonComboBoxes[button.ordinal].value)
        }
    }

    @FXML
    private fun apply() {
        apply(PRESETS[presetComboBox.value]!!)
    }

    @FXML
    private fun clearKeyBindings() {
        buttonComboBoxes.forEach { it.value = Key.UNDEFINED }
    }

    private fun apply(keyMapping: KeyMapping) {
        for (button in StandardControllerButton.values()) {
            buttonComboBoxes[button.ordinal].value = keyMapping.key(button)
        }
    }

    companion object {

        @JvmStatic private val PRESETS = mapOf(
            "WASD" to KeyMapping.wasd(),
            "ARROW" to KeyMapping.arrowKeys(),
        )
    }
}
