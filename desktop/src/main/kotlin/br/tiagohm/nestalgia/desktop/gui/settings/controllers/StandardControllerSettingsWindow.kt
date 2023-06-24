package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.core.StandardControllerButton
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

open class StandardControllerSettingsWindow(protected val keyMapping: KeyMapping) : AbstractWindow() {

    override val resourceName = "StandardControllerSettings"

    @FXML protected lateinit var upComboBox: ComboBox<Key>
    @FXML protected lateinit var downComboBox: ComboBox<Key>
    @FXML protected lateinit var leftComboBox: ComboBox<Key>
    @FXML protected lateinit var rightComboBox: ComboBox<Key>
    @FXML protected lateinit var startComboBox: ComboBox<Key>
    @FXML protected lateinit var selectComboBox: ComboBox<Key>
    @FXML protected lateinit var bComboBox: ComboBox<Key>
    @FXML protected lateinit var aComboBox: ComboBox<Key>
    @FXML protected lateinit var microphoneComboBox: ComboBox<Key>
    @FXML protected lateinit var turboBComboBox: ComboBox<Key>
    @FXML protected lateinit var turboAComboBox: ComboBox<Key>
    @FXML protected lateinit var presetComboBox: ComboBox<String>

    protected lateinit var buttonComboBoxes: Array<ComboBox<Key>>

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
    }

    override fun onStart() {
        apply(keyMapping)
    }

    override fun onStop() {
        for (button in StandardControllerButton.entries) {
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
        for (button in StandardControllerButton.entries) {
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
