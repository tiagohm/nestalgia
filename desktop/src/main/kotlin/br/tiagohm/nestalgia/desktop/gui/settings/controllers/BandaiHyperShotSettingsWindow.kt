package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.BandaiHyperShotButton.*
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class BandaiHyperShotSettingsWindow(keyMapping: KeyMapping) : StandardControllerSettingsWindow(keyMapping) {

    override val resourceName = "BandaiHyperShotSettings"

    @FXML private lateinit var fireComboBox: ComboBox<Key>
    @FXML private lateinit var aimOffscreenComboBox: ComboBox<Key>

    override fun onCreate() {
        super.onCreate()

        title = "Bandai Hyper Shot"

        fireComboBox.converter = KeyStringConverter
        aimOffscreenComboBox.converter = KeyStringConverter
    }

    override fun onStart() {
        super.onStart()

        fireComboBox.value = keyMapping.customKey(FIRE)
        aimOffscreenComboBox.value = keyMapping.customKey(1)
    }

    override fun onStop() {
        super.onStop()

        keyMapping.customKey(FIRE, fireComboBox.value)
        keyMapping.customKey(1, aimOffscreenComboBox.value)
    }
}
