package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.desktop.gui.AbstractDialog
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

data class StandardControllerSettingsWindow(private val keyMapping: KeyMapping) : AbstractDialog() {

    override val resourceName = "StandardControllerSettings"

    @FXML private lateinit var upComboBox: ComboBox<String>
    @FXML private lateinit var downComboBox: ComboBox<String>
    @FXML private lateinit var leftComboBox: ComboBox<String>
    @FXML private lateinit var rightComboBox: ComboBox<String>
    @FXML private lateinit var selectComboBox: ComboBox<String>
    @FXML private lateinit var startComboBox: ComboBox<String>
    @FXML private lateinit var aComboBox: ComboBox<String>
    @FXML private lateinit var bComboBox: ComboBox<String>
    @FXML private lateinit var presetComboBox: ComboBox<String>

    override fun onCreate() {
        title = "Standard Controller"
        resizable = false

        with(KEYS_BY_NAME.keys.sorted()) {
            upComboBox.items.addAll(this)
            downComboBox.items.addAll(this)
            leftComboBox.items.addAll(this)
            rightComboBox.items.addAll(this)
            selectComboBox.items.addAll(this)
            startComboBox.items.addAll(this)
            aComboBox.items.addAll(this)
            bComboBox.items.addAll(this)
        }

        reset()
    }

    @FXML
    private fun applyPreset() {
        val preset = if (presetComboBox.value == "WASD") WASD_LAYOUT else ARROW_KEYS_LAYOUT
        applyPreset(preset)
    }

    private fun applyPreset(keyMapping: KeyMapping) {
        upComboBox.value = KEYS_BY_CODE[keyMapping.up]
        downComboBox.value = KEYS_BY_CODE[keyMapping.down]
        leftComboBox.value = KEYS_BY_CODE[keyMapping.left]
        rightComboBox.value = KEYS_BY_CODE[keyMapping.right]
        selectComboBox.value = KEYS_BY_CODE[keyMapping.select]
        startComboBox.value = KEYS_BY_CODE[keyMapping.start]
        aComboBox.value = KEYS_BY_CODE[keyMapping.a]
        bComboBox.value = KEYS_BY_CODE[keyMapping.b]
    }

    @FXML
    private fun reset() {
        applyPreset(keyMapping)
    }

    @FXML
    private fun save() {
        keyMapping.up = KEYS_BY_NAME[upComboBox.value]!!
        keyMapping.down = KEYS_BY_NAME[downComboBox.value]!!
        keyMapping.left = KEYS_BY_NAME[leftComboBox.value]!!
        keyMapping.right = KEYS_BY_NAME[rightComboBox.value]!!
        keyMapping.select = KEYS_BY_NAME[selectComboBox.value]!!
        keyMapping.start = KEYS_BY_NAME[startComboBox.value]!!
        keyMapping.a = KEYS_BY_NAME[aComboBox.value]!!
        keyMapping.b = KEYS_BY_NAME[bComboBox.value]!!

        saved = true

        close()
    }

    companion object {

        @JvmStatic private val KEYS_BY_NAME = mapOf(
            "Undefined" to 0,
            "Backspace" to 8,
            "Tab" to 9,
            "Enter" to 10,
            "Space" to 32,
            "Page Up" to 33, "Page Down" to 34,
            "End" to 35,
            "Home" to 36,
            "Left" to 37, "Up" to 38, "Right" to 39, "Down" to 40,
            "Comma" to 44,
            "Minus" to 45,
            "Period" to 46,
            "Slash" to 47,
            "0" to 48, "1" to 49,
            "2" to 50, "3" to 51,
            "4" to 52, "5" to 53,
            "6" to 54, "7" to 55,
            "8" to 56, "9" to 57,
            "Semicolon" to 59,
            "Equals" to 61,
            "A" to 65, "B" to 66, "C" to 67,
            "D" to 68, "E" to 69, "F" to 70,
            "G" to 71, "H" to 72, "I" to 73,
            "J" to 74, "K" to 75, "L" to 76,
            "M" to 77, "N" to 78, "O" to 79,
            "P" to 80, "Q" to 81, "R" to 82,
            "S" to 83, "T" to 84, "U" to 85,
            "V" to 86, "W" to 87, "X" to 88,
            "Y" to 89, "Z" to 90,
            "Open Bracket" to 91,
            "Back Slash" to 92,
            "Close Bracket" to 93,
            "NumPad 0" to 96, "NumPad 1" to 97,
            "NumPad 2" to 98, "NumPad 3" to 99,
            "NumPad 4" to 100, "NumPad 5" to 101,
            "NumPad 6" to 102, "NumPad 7" to 103,
            "NumPad 8" to 104, "NumPad 9" to 105,
            "NumPad *" to 106, "NumPad +" to 107,
            "NumPad ," to 108, "NumPad -" to 109,
            "NumPad ." to 110, "NumPad /" to 111,
            "F1" to 112, "F2" to 113,
            "F3" to 114, "F4" to 115,
            "F5" to 116, "F6" to 117,
            "F7" to 118, "F8" to 119,
            "F9" to 120, "F10" to 121,
            "F11" to 122, "F12" to 123,
            "Delete" to 127,
        )

        @JvmStatic private val KEYS_BY_CODE = KEYS_BY_NAME
            .keys.associateBy { KEYS_BY_NAME[it]!! }

        @JvmStatic private val WASD_LAYOUT = KeyMapping(
            KEYS_BY_NAME["K"]!!, KEYS_BY_NAME["J"]!!,
            KEYS_BY_NAME["W"]!!, KEYS_BY_NAME["S"]!!,
            KEYS_BY_NAME["A"]!!, KEYS_BY_NAME["D"]!!,
            KEYS_BY_NAME["I"]!!, KEYS_BY_NAME["U"]!!,
        )

        @JvmStatic private val ARROW_KEYS_LAYOUT = KeyMapping(
            KEYS_BY_NAME["S"]!!, KEYS_BY_NAME["A"]!!,
            KEYS_BY_NAME["Up"]!!, KEYS_BY_NAME["Down"]!!,
            KEYS_BY_NAME["Left"]!!, KEYS_BY_NAME["Right"]!!,
            KEYS_BY_NAME["W"]!!, KEYS_BY_NAME["Q"]!!,
        )
    }
}
