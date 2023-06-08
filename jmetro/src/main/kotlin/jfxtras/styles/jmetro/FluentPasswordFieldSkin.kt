package jfxtras.styles.jmetro

import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent

class FluentPasswordFieldSkin(textField: TextField) : TextFieldWithButtonSkin(textField) {

    private var isMaskTextDisabled = false

    override fun onRightButtonPressed(event: MouseEvent) {
        isMaskTextDisabled = true
        skinnable.text = textField.text
        isMaskTextDisabled = false
    }

    override fun onRightButtonReleased(event: MouseEvent) {
        skinnable.text = skinnable.text
        skinnable.end()
    }

    override fun maskText(text: String): String {
        return if (skinnable is PasswordField && !isMaskTextDisabled) {
            val passwordBuilder = StringBuilder(text.length)
            for (i in text.indices) passwordBuilder.append(BULLET)
            "$passwordBuilder"
        } else {
            text
        }
    }

    companion object {

        private const val BULLET = '\u25cf'
    }
}
