package br.tiagohm.nestalgia.desktop.gui.converters

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.core.MouseButton
import javafx.util.StringConverter

object KeyStringConverter : StringConverter<Key>() {

    override fun toString(key: Key?) = when (key) {
        is KeyboardKeys -> key.description
        MouseButton.LEFT -> "Mouse Left"
        MouseButton.RIGHT -> "Mouse Right"
        MouseButton.MIDDLE -> "Mouse Middle"
        else -> "$key"
    }

    override fun fromString(text: String?) = null
}
