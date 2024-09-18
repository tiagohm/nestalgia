package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ArkanoidController
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider

class ArkanoidSettingsWindow(
    private val arkanoidSensibility: IntArray,
    override val keyMapping: KeyMapping,
    private val port: Int,
) : AbstractControllerWindow<ArkanoidController.Button>() {

    override val resourceName = "ArkanoidSettings"

    @FXML private lateinit var fireComboBox: ComboBox<Key>
    @FXML private lateinit var sensibilitySlider: Slider

    override lateinit var buttonComboBoxes: Array<ComboBox<Key>?>
    override val buttonEntries = ArkanoidController.Button.entries

    override fun onCreate() {
        title = "Arkanoid"

        buttonComboBoxes = arrayOf(fireComboBox)

        super.onCreate()
    }

    override fun onStart() {
        super.onStart()
        sensibilitySlider.value = arkanoidSensibility[port].toDouble()
    }

    override fun onStop() {
        super.onStop()
        arkanoidSensibility[port] = sensibilitySlider.value.toInt()
    }
}
