package jfxtras.styles.jmetro

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent

class FluentTextFieldSkin(textField: TextField) : TextFieldWithButtonSkin(textField) {

    init {
        textField.skinProperty().addListener(object : InvalidationListener {

            override fun invalidated(observable: Observable) {
                textField.applyCss()
                textField.skinProperty().removeListener(this)
            }
        })
    }

    override fun onRightButtonPressed(event: MouseEvent) {
        skinnable.text = ""
    }
}
