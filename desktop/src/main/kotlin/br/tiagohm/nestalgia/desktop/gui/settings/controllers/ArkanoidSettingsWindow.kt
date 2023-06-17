package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.MouseButton
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider

class ArkanoidSettingsWindow(
    private val arkanoidSensibility: IntArray,
    private val keyMapping: KeyMapping,
    private val port: Int,
) : AbstractWindow() {

    override val resourceName = "ArkanoidSettings"

    @FXML private lateinit var fireComboBox: ComboBox<MouseButton>
    @FXML private lateinit var sensibilitySlider: Slider

    override fun onCreate() {
        title = "Arkanoid"
        resizable = false
    }

    override fun onStart() {
        fireComboBox.value = keyMapping.arkanoidFire
        sensibilitySlider.value = arkanoidSensibility[port].toDouble()
    }

    override fun onStop() {
        keyMapping.arkanoidFire = fireComboBox.value
        arkanoidSensibility[port] = sensibilitySlider.value.toInt()
    }
}
