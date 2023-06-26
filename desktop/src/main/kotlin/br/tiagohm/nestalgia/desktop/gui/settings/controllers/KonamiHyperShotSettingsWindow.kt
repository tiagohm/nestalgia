package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.core.KonamiHyperShot
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class KonamiHyperShotSettingsWindow(private val keyMapping: KeyMapping) : AbstractWindow() {

    override val resourceName = "KonamiHyperShotSettings"

    @FXML private lateinit var jumpP1ComboBox: ComboBox<Key>
    @FXML private lateinit var runP1ComboBox: ComboBox<Key>
    @FXML private lateinit var jumpP2ComboBox: ComboBox<Key>
    @FXML private lateinit var runP2ComboBox: ComboBox<Key>

    private lateinit var buttonComboBoxes: Array<ComboBox<Key>>

    override fun onCreate() {
        title = "Konami Hyper Shot"
        resizable = false

        buttonComboBoxes = arrayOf(jumpP1ComboBox, runP1ComboBox, jumpP2ComboBox, runP2ComboBox)

        buttonComboBoxes.forEach { it.converter = KeyStringConverter }
        buttonComboBoxes.forEach { it.items.setAll(KeyboardKeys.SORTED_KEYS) }
    }

    override fun onStart() {
        for (button in KonamiHyperShot.Button.entries) {
            buttonComboBoxes[button.ordinal].value = keyMapping.customKey(button)
        }
    }

    override fun onStop() {
        for (button in KonamiHyperShot.Button.entries) {
            keyMapping.customKey(button, buttonComboBoxes[button.ordinal].value)
        }
    }

    @FXML
    private fun clearKeyBindings() {
        buttonComboBoxes.forEach { it.value = Key.UNDEFINED }
    }
}
