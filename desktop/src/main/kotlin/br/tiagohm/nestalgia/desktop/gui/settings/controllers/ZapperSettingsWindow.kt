package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.ZapperButton.*
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider

class ZapperSettingsWindow(
    private val zapperDetectionRadius: IntArray,
    private val keyMapping: KeyMapping,
    private val port: Int,
) : AbstractWindow() {

    override val resourceName = "ZapperSettings"

    @FXML private lateinit var fireComboBox: ComboBox<Key>
    @FXML private lateinit var aimOffscreenComboBox: ComboBox<Key>
    @FXML private lateinit var lightDetectionRadiusSlider: Slider

    override fun onCreate() {
        title = "Zapper"
        resizable = false
    }

    override fun onStart() {
        fireComboBox.value = keyMapping.customKey(FIRE)
        aimOffscreenComboBox.value = keyMapping.customKey(AIM_OFFSCREEN)
        lightDetectionRadiusSlider.value = zapperDetectionRadius[port].toDouble()
    }

    override fun onStop() {
        keyMapping.customKey(FIRE, fireComboBox.value)
        keyMapping.customKey(AIM_OFFSCREEN, aimOffscreenComboBox.value)
        zapperDetectionRadius[port] = lightDetectionRadiusSlider.value.toInt()
    }
}
