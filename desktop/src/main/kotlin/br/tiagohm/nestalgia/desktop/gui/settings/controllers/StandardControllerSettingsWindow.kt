package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

data class StandardControllerSettingsWindow(private val keyMapping: KeyMapping) : AbstractWindow() {

    override val resourceName = "StandardControllerSettings"

    @FXML private lateinit var upComboBox: ComboBox<Key>
    @FXML private lateinit var downComboBox: ComboBox<Key>
    @FXML private lateinit var leftComboBox: ComboBox<Key>
    @FXML private lateinit var rightComboBox: ComboBox<Key>
    @FXML private lateinit var selectComboBox: ComboBox<Key>
    @FXML private lateinit var startComboBox: ComboBox<Key>
    @FXML private lateinit var aComboBox: ComboBox<Key>
    @FXML private lateinit var bComboBox: ComboBox<Key>
    @FXML private lateinit var presetComboBox: ComboBox<String>

    override fun onCreate() {
        title = "NES/Famicom Controller"
        resizable = false

        upComboBox.converter = KeyStringConverter
        downComboBox.converter = KeyStringConverter
        leftComboBox.converter = KeyStringConverter
        rightComboBox.converter = KeyStringConverter
        selectComboBox.converter = KeyStringConverter
        startComboBox.converter = KeyStringConverter
        aComboBox.converter = KeyStringConverter
        bComboBox.converter = KeyStringConverter

        with(KeyboardKeys.SORTED_KEYS) {
            upComboBox.items.setAll(this)
            downComboBox.items.setAll(this)
            leftComboBox.items.setAll(this)
            rightComboBox.items.setAll(this)
            selectComboBox.items.setAll(this)
            startComboBox.items.setAll(this)
            aComboBox.items.setAll(this)
            bComboBox.items.setAll(this)
        }

        apply(keyMapping)
    }

    override fun onStop() {
        keyMapping.up = upComboBox.value!!
        keyMapping.down = downComboBox.value!!
        keyMapping.left = leftComboBox.value!!
        keyMapping.right = rightComboBox.value!!
        keyMapping.select = selectComboBox.value!!
        keyMapping.start = startComboBox.value!!
        keyMapping.a = aComboBox.value!!
        keyMapping.b = bComboBox.value!!
    }

    @FXML
    private fun apply() {
        apply(PRESETS[presetComboBox.value]!!)
    }

    private fun apply(keyMapping: KeyMapping) {
        upComboBox.value = keyMapping.up
        downComboBox.value = keyMapping.down
        leftComboBox.value = keyMapping.left
        rightComboBox.value = keyMapping.right
        selectComboBox.value = keyMapping.select
        startComboBox.value = keyMapping.start
        aComboBox.value = keyMapping.a
        bComboBox.value = keyMapping.b
    }

    companion object {

        @JvmStatic private val PRESETS = mapOf(
            "WASD" to KeyMapping.wasd(),
            "ARROW" to KeyMapping.arrowKeys(),
        )
    }
}
