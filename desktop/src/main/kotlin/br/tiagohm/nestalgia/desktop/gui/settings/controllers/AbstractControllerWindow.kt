package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerButton
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.ControllerButtonStringConverter
import br.tiagohm.nestalgia.desktop.gui.converters.KeyStringConverter
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

abstract class AbstractControllerWindow : AbstractWindow() {

    final override val resourceName = "Controller"

    @FXML protected lateinit var container: VBox
        private set

    @FXML protected lateinit var presetBox: HBox
        private set

    @FXML protected lateinit var presetComboBox: ComboBox<String>
        private set

    @FXML protected lateinit var buttonsBox: HBox
        private set

    private val comboBoxes = ArrayList<ComboBox<Key>>(32)
    private val initialKeyMapping = KeyMapping()

    protected abstract val keyMapping: KeyMapping
    protected abstract val defaultKeyMapping: KeyMapping
    protected abstract val buttons: Iterable<ControllerButton>
    protected open val presets: Map<String, KeyMapping> = emptyMap()

    protected open fun buttonKeys(button: ControllerButton): Collection<Key> = KeyboardKeys.SORTED_KEYS

    override fun onCreate() {
        super.onCreate()

        window.minWidth = 380.0

        keyMapping.copyInto(initialKeyMapping)
    }

    override fun onStart() {
        buttons.forEach(::addKey)

        presetBox.isVisible = presets.isNotEmpty()

        if (presets.isNotEmpty()) {
            presetComboBox.items = FXCollections.observableArrayList(presets.keys)
            presetComboBox.value = presetComboBox.items.first()
        }
    }

    private fun applyKeyMapping(keyMapping: KeyMapping) {
        for (box in comboBoxes) {
            val data = box.userData

            val key = if (data is ControllerButton) {
                keyMapping.key(data)
            } else if (data is Int) {
                keyMapping.customKey(data)
            } else {
                continue
            }

            if (key !== Key.UNDEFINED) {
                box.value = key
            }
        }
    }

    @FXML
    protected fun applyPreset() {
        val keyMapping = presets[presetComboBox.value] ?: return
        applyKeyMapping(keyMapping)
    }

    @FXML
    protected fun clear() {
        comboBoxes.forEach { it.value = Key.UNDEFINED }
    }

    @FXML
    protected fun reset() {
        applyKeyMapping(initialKeyMapping)
    }

    @FXML
    protected fun resetToDefault() {
        applyKeyMapping(defaultKeyMapping)
    }

    protected fun addKey(button: ControllerButton, keys: Collection<Key> = buttonKeys(button)) {
        addKey(ControllerButtonStringConverter.toString(button), keyMapping.key(button), button, keys)
    }

    protected fun addKey(label: String, key: Key?, data: Any? = null, keys: Collection<Key> = KeyboardKeys.SORTED_KEYS) {
        val hbox = HBox()

        with(Label(label)) {
            minWidth = 104.0
            alignment = Pos.CENTER_LEFT
            maxHeight = Double.POSITIVE_INFINITY
            hbox.children.add(this)
        }

        with(ComboBox(FXCollections.observableArrayList(keys))) {
            converter = KeyStringConverter
            minWidth = 220.0
            value = key
            userData = data
            hbox.children.add(this)
            comboBoxes.add(this)

            valueProperty().addListener { _, _, value ->
                if (data is ControllerButton) {
                    keyMapping.key(data, value)
                } else if (data is Int) {
                    keyMapping.customKey(data, value)
                }
            }
        }

        container.children.add(hbox)
    }

    protected fun addSlider(label: String, min: Double, max: Double, value: Double, changed: (Double) -> Unit) {
        val hbox = HBox()

        with(Label(label)) {
            minWidth = 64.0
            alignment = Pos.CENTER_LEFT
            maxHeight = Double.POSITIVE_INFINITY
            hbox.children.add(this)
        }

        with(Slider(min, max, value)) {
            HBox.setHgrow(this, Priority.ALWAYS)
            isShowTickMarks = true
            isShowTickLabels = true
            isSnapToTicks = true
            majorTickUnit = 1.0
            maxWidth = Double.POSITIVE_INFINITY
            hbox.children.add(this)

            valueProperty().addListener { _, _, value -> changed(value.toDouble()) }
        }

        container.children.add(hbox)
    }
}
