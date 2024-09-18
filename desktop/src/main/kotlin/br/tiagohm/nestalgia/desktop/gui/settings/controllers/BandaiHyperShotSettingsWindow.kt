package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.BandaiHyperShot.Button.FIRE
import br.tiagohm.nestalgia.core.BandaiHyperShot.Companion.AIM_OFFSCREEN_CUSTOM_KEY
import br.tiagohm.nestalgia.core.ControllerType.BANDAI_HYPER_SHOT
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

class BandaiHyperShotSettingsWindow(keyMapping: KeyMapping) : StandardControllerSettingsWindow(keyMapping, BANDAI_HYPER_SHOT) {

    override val resourceName = "BandaiHyperShotSettings"

    @FXML private lateinit var fireComboBox: ComboBox<Key>
    @FXML private lateinit var aimOffscreenComboBox: ComboBox<Key>

    override fun onCreate() {
        super.onCreate()

        fireComboBox.initialize()
        aimOffscreenComboBox.initialize()
    }

    override fun onStart() {
        super.onStart()

        fireComboBox.value = keyMapping.customKey(FIRE)
        aimOffscreenComboBox.value = keyMapping.customKey(AIM_OFFSCREEN_CUSTOM_KEY)
    }

    override fun onStop() {
        super.onStop()

        keyMapping.customKey(FIRE, fireComboBox.value)
        keyMapping.customKey(AIM_OFFSCREEN_CUSTOM_KEY, aimOffscreenComboBox.value)
    }
}
