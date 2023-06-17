package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.MouseButton
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

    @FXML private lateinit var fireComboBox: ComboBox<MouseButton>
    @FXML private lateinit var aimOffscreenComboBox: ComboBox<MouseButton>
    @FXML private lateinit var lightDetectionRadiusSlider: Slider

    override fun onCreate() {
        title = "Zapper"
        resizable = false
    }

    override fun onStart() {
        fireComboBox.value = keyMapping.zapperFire
        aimOffscreenComboBox.value = keyMapping.zapperAimOffscreen
        lightDetectionRadiusSlider.value = zapperDetectionRadius[port].toDouble()
    }

    override fun onStop() {
        keyMapping.zapperFire = fireComboBox.value
        keyMapping.zapperAimOffscreen = aimOffscreenComboBox.value
        zapperDetectionRadius[port] = lightDetectionRadiusSlider.value.toInt()
    }
}
