package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.MouseButton
import br.tiagohm.nestalgia.desktop.gui.AbstractDialog
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider

class ZapperSettingsWindow(
    private val zapperDetectionRadius: IntArray,
    private val player: Int,
) : AbstractDialog() {

    override val resourceName = "ZapperSettings"

    @FXML private lateinit var fireComboBox: ComboBox<MouseButton>
    @FXML private lateinit var aimOffscreenComboBox: ComboBox<MouseButton>
    @FXML private lateinit var lightDetectionRadiusSlider: Slider

    override fun onCreate() {
        title = "Zapper"
        resizable = false

        reset()
    }

    @FXML
    private fun reset() {
        lightDetectionRadiusSlider.value = zapperDetectionRadius[player].toDouble()
    }

    @FXML
    private fun save() {
        zapperDetectionRadius[player] = lightDetectionRadiusSlider.value.toInt()
        saved = true
        close()
    }
}
