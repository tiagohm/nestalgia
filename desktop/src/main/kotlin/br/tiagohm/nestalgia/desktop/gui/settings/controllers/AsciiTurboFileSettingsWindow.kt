package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.desktop.gui.AbstractDialog
import javafx.fxml.FXML
import javafx.scene.control.Spinner

class AsciiTurboFileSettingsWindow(var slot: Int) : AbstractDialog() {

    override val resourceName = "AsciiTurboFileSettings"

    @FXML private lateinit var slotSpinner: Spinner<Double>

    override fun onCreate() {
        title = "Ascii Turbo File"
        resizable = false

        reset()
    }

    @FXML
    private fun reset() {
        slotSpinner.valueFactory.value = 0.0
    }

    @FXML
    private fun save() {
        slot = slotSpinner.value.toInt()
        saved = true
        close()
    }
}
