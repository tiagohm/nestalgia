package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerType
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.Pachinko.Button.PRESS
import br.tiagohm.nestalgia.core.Pachinko.Button.RELEASE
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

open class PachinkoSettingsWindow(keyMapping: KeyMapping) : StandardControllerSettingsWindow(keyMapping, ControllerType.PACHINKO) {

    override val resourceName = "PachinkoSettings"

    @FXML protected lateinit var pressComboBox: ComboBox<Key>
    @FXML protected lateinit var releaseComboBox: ComboBox<Key>

    override fun onCreate() {
        super.onCreate()

        pressComboBox.initialize()
        releaseComboBox.initialize()
    }

    override fun onStart() {
        super.onStart()
        pressComboBox.value = keyMapping.customKey(PRESS)
        releaseComboBox.value = keyMapping.customKey(RELEASE)
    }

    override fun onStop() {
        super.onStop()
        keyMapping.key(PRESS, pressComboBox.value)
        keyMapping.key(RELEASE, releaseComboBox.value)
    }
}
