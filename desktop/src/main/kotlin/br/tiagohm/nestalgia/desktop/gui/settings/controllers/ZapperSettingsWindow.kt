package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.MouseButton
import br.tiagohm.nestalgia.core.Zapper
import br.tiagohm.nestalgia.core.Zapper.Companion.AIM_OFFSCREEN_CUSTOM_KEY
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider

class ZapperSettingsWindow(
    private val zapperDetectionRadius: IntArray,
    override val keyMapping: KeyMapping,
    private val port: Int,
) : AbstractControllerWindow<Zapper.Button>() {

    override val resourceName = "ZapperSettings"

    @FXML private lateinit var fireComboBox: ComboBox<Key>
    @FXML private lateinit var aimOffscreenComboBox: ComboBox<Key>
    @FXML private lateinit var lightDetectionRadiusSlider: Slider

    override lateinit var buttonComboBoxes: Array<ComboBox<Key>?>
    override val buttonEntries = Zapper.Button.entries

    override fun buttonKeys(button: Zapper.Button) = MouseButton.entries

    override fun onCreate() {
        title = "Zapper"

        buttonComboBoxes = arrayOf(fireComboBox)

        super.onCreate()

        aimOffscreenComboBox.initialize(MouseButton.entries)
    }

    override fun onStart() {
        super.onStart()
        aimOffscreenComboBox.value = keyMapping.customKey(AIM_OFFSCREEN_CUSTOM_KEY)
        lightDetectionRadiusSlider.value = zapperDetectionRadius[port].toDouble()
    }

    override fun onStop() {
        super.onStop()
        keyMapping.customKey(AIM_OFFSCREEN_CUSTOM_KEY, aimOffscreenComboBox.value)
        zapperDetectionRadius[port] = lightDetectionRadiusSlider.value.toInt()
    }
}
