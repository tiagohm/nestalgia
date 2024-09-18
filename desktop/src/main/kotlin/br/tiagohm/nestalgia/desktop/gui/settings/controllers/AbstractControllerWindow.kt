package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerButton
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

abstract class AbstractControllerWindow<T> : AbstractWindow() where T : Enum<*>, T : ControllerButton {

    protected abstract val keyMapping: KeyMapping
    protected abstract val buttonComboBoxes: Array<ComboBox<Key>?>
    protected abstract val buttonEntries: Iterable<T>

    protected open val presetComboBox: ComboBox<String>? = null

    override fun onCreate() {
        super.onCreate()

        resizable = false

        buttonComboBoxes.forEach { it?.initialize() }
    }

    override fun onStart() {
        for (button in buttonEntries) {
            buttonComboBoxes[button.ordinal]?.value = keyMapping.key(button)
        }
    }

    override fun onStop() {
        for (button in buttonEntries) {
            keyMapping.key(button, buttonComboBoxes[button.ordinal]?.value ?: continue)
        }
    }

    @FXML
    protected fun apply() {
        val value = presetComboBox?.value?.takeIf { it.isNotBlank() } ?: return
        apply(PRESETS[value] ?: return)
    }

    @FXML
    protected fun clearKeyBindings() {
        buttonComboBoxes.forEach { it?.value = Key.UNDEFINED }
    }

    protected fun apply(keyMapping: KeyMapping) {
        for (button in buttonEntries) {
            buttonComboBoxes[button.ordinal]?.value = keyMapping.key(button)
        }
    }

    protected fun ComboBox<Key>.initialize() {
        converter = KeyStringConverter
        items.setAll(KeyboardKeys.SORTED_KEYS)
    }

    companion object {

        private val PRESETS = mapOf(
            "WASD" to KeyMapping.wasd(),
            "ARROW" to KeyMapping.arrowKeys(),
        )
    }
}
