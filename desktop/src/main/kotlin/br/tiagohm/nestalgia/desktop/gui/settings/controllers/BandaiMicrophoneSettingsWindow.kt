package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.BandaiMicrophone
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

open class BandaiMicrophoneSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow<BandaiMicrophone.Button>() {

    override val resourceName = "BandaiMicrophoneSettings"

    @FXML protected lateinit var aComboBox: ComboBox<Key>
    @FXML protected lateinit var bComboBox: ComboBox<Key>
    @FXML protected lateinit var microphoneComboBox: ComboBox<Key>

    override lateinit var buttonComboBoxes: Array<ComboBox<Key>?>
    override val buttonEntries = BandaiMicrophone.Button.entries

    override fun onCreate() {
        title = "Bandai Microphone"

        buttonComboBoxes = arrayOf(aComboBox, bComboBox, microphoneComboBox)

        super.onCreate()
    }
}
