package br.tiagohm.nestalgia.desktop.gui.barcode

import br.tiagohm.nestalgia.desktop.console
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import java.util.function.UnaryOperator

class BarcodeInputWindow : AbstractWindow(), UnaryOperator<TextFormatter.Change> {

    override val resourceName = "BarcodeInput"

    @FXML private lateinit var barcodeTextField: TextField

    override fun onCreate() {
        title = "Barcode Input"

        resizable = false

        barcodeTextField.textFormatter = TextFormatter<UnaryOperator<TextFormatter.Change>>(this)
    }

    override fun apply(change: TextFormatter.Change): TextFormatter.Change {
        if (change.isAdded) {
            var newText = change.controlNewText

            if (newText.any { !it.isDigit() }) {
                newText = newText.replace(NON_DIGIT_REGEX, "")
            }

            if (newText.length > 13) {
                newText = newText.substring(0, 13)
            }

            change.text = newText
            change.setRange(0, change.controlText.length)
        }

        return change
    }

    @FXML
    private fun apply() {
        val barcodeText = barcodeTextField.text

        if (barcodeText.isNotBlank()) {
            val digitCount = if (barcodeText.length > 8) 13 else 8
            console.inputBarcode(barcodeText.toLong(), digitCount)
            close()
        }
    }

    companion object {

        private val NON_DIGIT_REGEX = Regex("\\D+")
    }
}
