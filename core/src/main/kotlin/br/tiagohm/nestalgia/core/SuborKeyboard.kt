package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.SUBOR_KEYBOARD
import br.tiagohm.nestalgia.core.SuborKeyboard.Button.*

class SuborKeyboard(console: Console, keyMapping: KeyMapping) : ControlDevice(console, SUBOR_KEYBOARD, EXP_DEVICE_PORT) {

    enum class Button : ControllerButton, HasCustomKey {
        A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
        NUMBER_0, NUMBER_1, NUMBER_2, NUMBER_3, NUMBER_4, NUMBER_5, NUMBER_6, NUMBER_7, NUMBER_8, NUMBER_9,
        F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
        NUMPAD_0, NUMPAD_1, NUMPAD_2, NUMPAD_3, NUMPAD_4, NUMPAD_5, NUMPAD_6, NUMPAD_7, NUMPAD_8, NUMPAD_9,
        NUMPAD_ENTER, NUMPAD_DOT, NUMPAD_PLUS, NUMPAD_TIMES, NUMPAD_SLASH, NUMPAD_MINUS, NUM_LOCK,
        COMMA, PERIOD, SEMICOLON, QUOTE,
        SLASH, BACK_SLASH,
        EQUALS, MINUS, GRAVE,
        OPEN_BRACKET, CLOSE_BRACKET,
        CAPS_LOCK, PAUSE,
        CTRL, SHIFT, ALT,
        SPACE, BACK_SPACE, TAB, ESC, ENTER,
        END, HOME,
        INS, DELETE,
        PAGE_UP, PAGE_DOWN,
        UP, DOWN, LEFT, RIGHT,
        UNKNOWN1, UNKNOWN2, UNKNOWN3, NONE;

        override val bit = ordinal
        override val keyIndex = 100 + ordinal
    }

    private val keys = Button.entries.map(keyMapping::customKey).toTypedArray()

    @Volatile private var row = 0
    @Volatile private var column = 0
    @Volatile private var enabled = false

    private val keyboardMatrix = arrayOf(
        NUMBER_4, G, F, C, F2, E, NUMBER_5, V,
        NUMBER_2, D, S, END, F1, W, NUMBER_3, X,
        INS, BACK_SPACE, PAGE_DOWN, RIGHT, F8, PAGE_UP, DELETE, HOME,
        NUMBER_9, I, L, COMMA, F5, O, NUMBER_0, PERIOD,
        CLOSE_BRACKET, ENTER, UP, LEFT, F7, OPEN_BRACKET, BACK_SLASH, DOWN,
        Q, CAPS_LOCK, Z, TAB, ESC, A, NUMBER_1, CTRL,
        NUMBER_7, Y, K, M, F4, U, NUMBER_8, J,
        MINUS, SEMICOLON, QUOTE, SLASH, F6, P, EQUALS, SHIFT,
        T, H, N, SPACE, F3, R, NUMBER_6, B,
        NUMPAD_6, NUMPAD_ENTER, NUMPAD_4, NUMPAD_8, NONE, UNKNOWN1, UNKNOWN2, UNKNOWN3,
        ALT, NUMPAD_4, NUMPAD_7, F11, F12, NUMPAD_1, NUMPAD_2, NUMPAD_8,
        NUMPAD_MINUS, NUMPAD_PLUS, NUMPAD_TIMES, NUMPAD_9, F10, NUMPAD_5, NUMPAD_SLASH, NUM_LOCK,
        GRAVE, NUMPAD_6, PAUSE, SPACE, F9, NUMPAD_3, NUMPAD_DOT, NUMPAD_0
    )

    private fun activeKeys(row: Int, column: Int): Int {
        var result = 0
        val baseIndex = row * 8 + (if (column > 0) 4 else 0)

        repeat(4) {
            if (isPressed(keyboardMatrix[baseIndex + it])) {
                result = result or (1 shl it)
            }
        }

        if (row == 9 && column > 0) {
            // This bit is used to indicate that this is an "extended" version of
            // the keyboard which has four more rows to read from (numpad, etc.)
            // Corresponds to row 9, bit 4
            result = result or 0x01
        }

        return result
    }

    override fun setStateFromInput() {
        Button.entries.forEach { setPressedState(it, keys[it.ordinal]) }
    }

    override fun refreshStateBuffer() {
        row = 0
        column = 0
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr == 0x4017) {
            if (enabled) {
                activeKeys(row, column).inv() shl 1 and 0x1E
            } else {
                0x1E
            }
        } else {
            0
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobeOnWrite(value)

        val prevColumn = column

        column = value and 0x02 shr 1
        enabled = value.bit2

        if (enabled) {
            if (column == 0 && prevColumn != 0) {
                row = (row + 1) % 13
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("row", row)
        s.write("column", column)
        s.write("enabled", enabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        row = s.readInt("row")
        column = s.readInt("column")
        enabled = s.readBoolean("enabled")
    }

    companion object : HasDefaultKeyMapping {

        override fun populateWithDefault(keyMapping: KeyMapping) {
            keyMapping.customKey(A, KeyboardKeys.A)
            keyMapping.customKey(B, KeyboardKeys.B)
            keyMapping.customKey(C, KeyboardKeys.C)
            keyMapping.customKey(D, KeyboardKeys.D)
            keyMapping.customKey(E, KeyboardKeys.E)
            keyMapping.customKey(F, KeyboardKeys.F)
            keyMapping.customKey(G, KeyboardKeys.G)
            keyMapping.customKey(H, KeyboardKeys.H)
            keyMapping.customKey(I, KeyboardKeys.I)
            keyMapping.customKey(J, KeyboardKeys.J)
            keyMapping.customKey(K, KeyboardKeys.K)
            keyMapping.customKey(L, KeyboardKeys.L)
            keyMapping.customKey(M, KeyboardKeys.M)
            keyMapping.customKey(N, KeyboardKeys.N)
            keyMapping.customKey(O, KeyboardKeys.O)
            keyMapping.customKey(P, KeyboardKeys.P)
            keyMapping.customKey(Q, KeyboardKeys.Q)
            keyMapping.customKey(R, KeyboardKeys.R)
            keyMapping.customKey(S, KeyboardKeys.S)
            keyMapping.customKey(T, KeyboardKeys.T)
            keyMapping.customKey(U, KeyboardKeys.U)
            keyMapping.customKey(V, KeyboardKeys.V)
            keyMapping.customKey(W, KeyboardKeys.W)
            keyMapping.customKey(X, KeyboardKeys.X)
            keyMapping.customKey(Y, KeyboardKeys.Y)
            keyMapping.customKey(Z, KeyboardKeys.Z)
            keyMapping.customKey(NUMBER_0, KeyboardKeys.NUMBER_0)
            keyMapping.customKey(NUMBER_1, KeyboardKeys.NUMBER_1)
            keyMapping.customKey(NUMBER_2, KeyboardKeys.NUMBER_2)
            keyMapping.customKey(NUMBER_3, KeyboardKeys.NUMBER_3)
            keyMapping.customKey(NUMBER_4, KeyboardKeys.NUMBER_4)
            keyMapping.customKey(NUMBER_5, KeyboardKeys.NUMBER_5)
            keyMapping.customKey(NUMBER_6, KeyboardKeys.NUMBER_6)
            keyMapping.customKey(NUMBER_7, KeyboardKeys.NUMBER_7)
            keyMapping.customKey(NUMBER_8, KeyboardKeys.NUMBER_8)
            keyMapping.customKey(NUMBER_9, KeyboardKeys.NUMBER_9)
            keyMapping.customKey(F1, KeyboardKeys.F1)
            keyMapping.customKey(F2, KeyboardKeys.F2)
            keyMapping.customKey(F3, KeyboardKeys.F3)
            keyMapping.customKey(F4, KeyboardKeys.F4)
            keyMapping.customKey(F5, KeyboardKeys.F5)
            keyMapping.customKey(F6, KeyboardKeys.F6)
            keyMapping.customKey(F7, KeyboardKeys.F7)
            keyMapping.customKey(F8, KeyboardKeys.F8)
            keyMapping.customKey(F9, KeyboardKeys.F9)
            keyMapping.customKey(F10, KeyboardKeys.F10)
            keyMapping.customKey(F11, KeyboardKeys.F11)
            keyMapping.customKey(F12, KeyboardKeys.F12)
            keyMapping.customKey(NUMPAD_0, KeyboardKeys.NUMPAD_0)
            keyMapping.customKey(NUMPAD_1, KeyboardKeys.NUMPAD_1)
            keyMapping.customKey(NUMPAD_2, KeyboardKeys.NUMPAD_2)
            keyMapping.customKey(NUMPAD_3, KeyboardKeys.NUMPAD_3)
            keyMapping.customKey(NUMPAD_4, KeyboardKeys.NUMPAD_4)
            keyMapping.customKey(NUMPAD_5, KeyboardKeys.NUMPAD_5)
            keyMapping.customKey(NUMPAD_6, KeyboardKeys.NUMPAD_6)
            keyMapping.customKey(NUMPAD_7, KeyboardKeys.NUMPAD_7)
            keyMapping.customKey(NUMPAD_8, KeyboardKeys.NUMPAD_8)
            keyMapping.customKey(NUMPAD_9, KeyboardKeys.NUMPAD_9)
            keyMapping.customKey(NUMPAD_ENTER, KeyboardKeys.ENTER)
            keyMapping.customKey(NUMPAD_DOT, KeyboardKeys.NUMPAD_DOT)
            keyMapping.customKey(NUMPAD_PLUS, KeyboardKeys.NUMPAD_PLUS)
            keyMapping.customKey(NUMPAD_TIMES, KeyboardKeys.NUMPAD_TIMES)
            keyMapping.customKey(NUMPAD_SLASH, KeyboardKeys.NUMPAD_SLASH)
            keyMapping.customKey(NUMPAD_MINUS, KeyboardKeys.NUMPAD_MINUS)
            keyMapping.customKey(NUM_LOCK, KeyboardKeys.NUM_LOCK)
            keyMapping.customKey(COMMA, KeyboardKeys.COMMA)
            keyMapping.customKey(PERIOD, KeyboardKeys.PERIOD)
            keyMapping.customKey(SEMICOLON, KeyboardKeys.SEMICOLON)
            keyMapping.customKey(QUOTE, KeyboardKeys.QUOTE)
            keyMapping.customKey(SLASH, KeyboardKeys.SLASH)
            keyMapping.customKey(BACK_SLASH, KeyboardKeys.BACK_SLASH)
            keyMapping.customKey(EQUALS, KeyboardKeys.EQUALS)
            keyMapping.customKey(MINUS, KeyboardKeys.MINUS)
            keyMapping.customKey(GRAVE, KeyboardKeys.DEAD_GRAVE)
            keyMapping.customKey(OPEN_BRACKET, KeyboardKeys.OPEN_BRACKET)
            keyMapping.customKey(CLOSE_BRACKET, KeyboardKeys.CLOSE_BRACKET)
            keyMapping.customKey(CAPS_LOCK, KeyboardKeys.CAPS_LOCK)
            keyMapping.customKey(PAUSE, KeyboardKeys.PAUSE)
            keyMapping.customKey(CTRL, KeyboardKeys.CTRL)
            keyMapping.customKey(SHIFT, KeyboardKeys.SHIFT)
            keyMapping.customKey(ALT, KeyboardKeys.ALT)
            keyMapping.customKey(SPACE, KeyboardKeys.SPACE)
            keyMapping.customKey(BACK_SPACE, KeyboardKeys.BACK_SPACE)
            keyMapping.customKey(TAB, KeyboardKeys.TAB)
            keyMapping.customKey(ESC, KeyboardKeys.ESC)
            keyMapping.customKey(ENTER, KeyboardKeys.ENTER)
            keyMapping.customKey(END, KeyboardKeys.END)
            keyMapping.customKey(HOME, KeyboardKeys.HOME)
            keyMapping.customKey(INS, KeyboardKeys.INS)
            keyMapping.customKey(DELETE, KeyboardKeys.DELETE)
            keyMapping.customKey(PAGE_UP, KeyboardKeys.PAGE_UP)
            keyMapping.customKey(PAGE_DOWN, KeyboardKeys.PAGE_DOWN)
            keyMapping.customKey(UP, KeyboardKeys.UP)
            keyMapping.customKey(DOWN, KeyboardKeys.DOWN)
            keyMapping.customKey(LEFT, KeyboardKeys.LEFT)
            keyMapping.customKey(RIGHT, KeyboardKeys.RIGHT)
            keyMapping.customKey(UNKNOWN1, KeyboardKeys.UNDEFINED)
            keyMapping.customKey(UNKNOWN2, KeyboardKeys.UNDEFINED)
            keyMapping.customKey(UNKNOWN3, KeyboardKeys.UNDEFINED)
            keyMapping.customKey(NONE, KeyboardKeys.UNDEFINED)
        }
    }
}
