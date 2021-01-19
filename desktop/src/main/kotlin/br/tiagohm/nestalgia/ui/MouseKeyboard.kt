package br.tiagohm.nestalgia.ui

import br.tiagohm.nestalgia.core.*
import java.awt.event.*
import java.util.*
import javax.swing.SwingUtilities

@ExperimentalUnsignedTypes
class MouseKeyboard(
    val console: Console,
    val renderer: RenderingDevice,
) : KeyManager,
    KeyListener,
    MouseListener,
    MouseMotionListener {

    private val keyPressed = HashMap<Int, Boolean>(65536)
    private var mouseButton = HashMap<MouseButton, Boolean>(3)

    init {
        for (i in 0..65535) keyPressed[i] = false
    }

    override var x = -1

    override var y = -1

    override fun isKeyPressed(keyCode: Int): Boolean {
        return keyPressed[keyCode] ?: false
    }

    override fun isMouseButtonPressed(mouseButton: MouseButton): Boolean {
        return this.mouseButton[mouseButton] ?: false
    }

    override fun refreshKeyState() {}

    override val keyNames = KEY_NAMES.toMutableList().also { it.sort() }

    override fun getKeyCode(name: String): Int {
        val index = KEY_NAMES.indexOf(name)
        return if (index >= 0) KEY_CODES[index] else 0
    }

    override fun getKeyName(keyCode: Int): String {
        val index = KEY_CODES.indexOf(keyCode)
        return if (index >= 0) KEY_NAMES[index] else "Undefined"
    }

    override fun keyTyped(e: KeyEvent) {}

    override fun keyPressed(e: KeyEvent) {
        keyPressed[e.keyCode] = true
    }

    override fun keyReleased(e: KeyEvent) {
        keyPressed[e.keyCode] = false
    }

    override fun mouseClicked(e: MouseEvent) {}

    override fun mousePressed(e: MouseEvent) {
        mouseButton[MouseButton.LEFT] = SwingUtilities.isLeftMouseButton(e)
        mouseButton[MouseButton.RIGHT] = SwingUtilities.isRightMouseButton(e)
        mouseButton[MouseButton.MIDDLE] = SwingUtilities.isMiddleMouseButton(e)
    }

    override fun mouseReleased(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) mouseButton[MouseButton.LEFT] = false
        if (SwingUtilities.isRightMouseButton(e)) mouseButton[MouseButton.RIGHT] = false
        if (SwingUtilities.isMiddleMouseButton(e)) mouseButton[MouseButton.MIDDLE] = false
    }

    override fun mouseEntered(e: MouseEvent) {}

    override fun mouseExited(e: MouseEvent) {}

    override fun mouseDragged(e: MouseEvent) {}

    override fun mouseMoved(e: MouseEvent) {
        x = ((e.x / renderer.screenWidth.toFloat()) * Ppu.SCREEN_WIDTH).toInt()
        y = ((e.y / renderer.screenHeight.toFloat()) * Ppu.SCREEN_HEIGHT).toInt()
    }

    companion object {
        private val KEY_CODES = intArrayOf(
            0, 3, 8, 9, 10, 12, 16, 17,
            18, 19, 20, 21, 24, 25, 27, 28,
            29, 30, 31, 32, 33, 34, 35, 36,
            37, 38, 39, 40, 44, 45, 46, 47,
            48, 49, 50, 51, 52, 53, 54, 55,
            56, 57, 59, 61, 65, 66, 67, 68,
            69, 70, 71, 72, 73, 74, 75, 76,
            77, 78, 79, 80, 81, 82, 83, 84,
            85, 86, 87, 88, 89, 90, 91, 92,
            93, 96, 97, 98, 99, 100, 101, 102,
            103, 104, 105, 106, 107, 108, 109, 110,
            111, 112, 113, 114, 115, 116, 117, 118,
            119, 120, 121, 122, 123, 127, 128, 129,
            130, 131, 132, 133, 134, 135, 136, 137,
            138, 139, 140, 141, 142, 143, 144, 145,
            150, 151, 152, 153, 154, 155, 156, 157,
            160, 161, 162, 192, 222, 224, 225, 226,
            227, 240, 241, 242, 243, 244, 245, 256,
            257, 258, 259, 260, 261, 262, 263, 512,
            513, 514, 515, 516, 517, 518, 519, 520,
            521, 522, 523, 524, 525, 61440, 61441, 61442,
            61443, 61444, 61445, 61446, 61447, 61448, 61449, 61450,
            61451, 65312, 65368, 65406, 65480, 65481, 65482, 65483,
            65485, 65487, 65488, 65489
        )

        private val KEY_NAMES = arrayOf(
            "Undefined", "Cancel", "Backspace", "Tab",
            "Enter", "Clear", "Shift", "Ctrl",
            "Alt", "Pause", "Caps Lock", "Kana",
            "Final", "Kanji", "Escape", "Convert",
            "No Convert", "Accept", "Mode Change", "Space",
            "Page Up", "Page Down", "End", "Home",
            "Left", "Up", "Right", "Down",
            "Comma", "Minus", "Period", "Slash",
            "0", "1", "2", "3",
            "4", "5", "6", "7",
            "8", "9", "Semicolon", "Equals",
            "A", "B", "C", "D",
            "E", "F", "G", "H",
            "I", "J", "K", "L",
            "M", "N", "O", "P",
            "Q", "R", "S", "T",
            "U", "V", "W", "X",
            "Y", "Z", "Open Bracket", "Back Slash",
            "Close Bracket", "NumPad 0", "NumPad 1", "NumPad 2",
            "NumPad 3", "NumPad 4", "NumPad 5", "NumPad 6",
            "NumPad 7", "NumPad 8", "NumPad 9", "NumPad *",
            "NumPad +", "NumPad ,", "NumPad -", "NumPad .",
            "NumPad /", "F1", "F2", "F3",
            "F4", "F5", "F6", "F7",
            "F8", "F9", "F10", "F11",
            "F12", "Delete", "Dead Grave", "Dead Acute",
            "Dead Circumflex", "Dead Tilde", "Dead Macron", "Dead Breve",
            "Dead Above Dot", "Dead Diaeresis", "Dead Above Ring", "Dead Double Acute",
            "Dead Caron", "Dead Cedilla", "Dead Ogonek", "Dead Iota",
            "Dead Voiced Sound", "Dead Semivoiced Sound", "Num Lock", "Scroll Lock",
            "Ampersand", "Asterisk", "Double Quote", "Less",
            "Print Screen", "Insert", "Help", "Meta",
            "Greater", "Left Brace", "Right Brace", "Back Quote",
            "Quote", "Up", "Down", "Left",
            "Right", "Alphanumeric", "Katakana", "Hiragana",
            "Full-Width", "Half-Width", "Roman Characters", "All Candidates",
            "Previous Candidate", "Code Input", "Japanese Katakana", "Japanese Hiragana",
            "Japanese Roman", "Kana Lock", "Input Method On/Off", "At",
            "Colon", "Circumflex", "Dollar", "Euro",
            "Exclamation Mark", "Inverted Exclamation Mark", "Left Parenthesis", "Number Sign",
            "Plus", "Right Parenthesis", "Underscore", "Windows",
            "Context Menu", "F13", "F14", "F15",
            "F16", "F17", "F18", "F19",
            "F20", "F21", "F22", "F23",
            "F24", "Compose", "Begin", "Alt Graph",
            "Stop", "Again", "Props", "Undo",
            "Copy", "Paste", "Find", "Cut",
        )
    }
}