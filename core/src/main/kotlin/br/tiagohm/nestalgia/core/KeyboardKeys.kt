package br.tiagohm.nestalgia.core

enum class KeyboardKeys(val description: String, override val code: Int) : Key {
    UNDEFINED("Undefined", 0),
    BACKSPACE("Backspace", 8),
    TAB("Tab", 9),
    ENTER("Enter", 10),
    SPACE("Space", 32),
    PAGE_UP("Page Up", 33),
    PAGE_DOWN("Page Down", 34),
    END("End", 35),
    HOME("Home", 36),
    LEFT("Left", 37),
    UP("Up", 38),
    RIGHT("Right", 39),
    DOWN("Down", 40),
    COMMA("Comma", 44),
    MINUS("Minus", 45),
    PERIOD("Period", 46),
    SLASH("Slash", 47),
    NUMBER_0("0", 48),
    NUMBER_1("1", 49),
    NUMBER_2("2", 50),
    NUMBER_3("3", 51),
    NUMBER_4("4", 52),
    NUMBER_5("5", 53),
    NUMBER_6("6", 54),
    NUMBER_7("7", 55),
    NUMBER_8("8", 56),
    NUMBER_9("9", 57),
    SEMICOLON("Semicolon", 59),
    EQUALS("Equals", 61),
    A("A", 65),
    B("B", 66),
    C("C", 67),
    D("D", 68),
    E("E", 69),
    F("F", 70),
    G("G", 71),
    H("H", 72),
    I("I", 73),
    J("J", 74),
    K("K", 75),
    L("L", 76),
    M("M", 77),
    N("N", 78),
    O("O", 79),
    P("P", 80),
    Q("Q", 81),
    R("R", 82),
    S("S", 83),
    T("T", 84),
    U("U", 85),
    V("V", 86),
    W("W", 87),
    X("X", 88),
    Y("Y", 89),
    Z("Z", 90),
    OPEN_BRACKET("Open Bracket", 91),
    BACK_SLASH("Back Slash", 92),
    CLOSE_BRACKET("Close Bracket", 93),
    NUMPAD_0("NumPad 0", 96),
    NUMPAD_1("NumPad 1", 97),
    NUMPAD_2("NumPad 2", 98),
    NUMPAD_3("NumPad 3", 99),
    NUMPAD_4("NumPad 4", 100),
    NUMPAD_5("NumPad 5", 101),
    NUMPAD_6("NumPad 6", 102),
    NUMPAD_7("NumPad 7", 103),
    NUMPAD_8("NumPad 8", 104),
    NUMPAD_9("NumPad 9", 105),
    NUMPAD_TIMES("NumPad *", 106),
    NUMPAD_PLUS("NumPad +", 107),
    NUMPAD_COMMA("NumPad ,", 108),
    NUMPAD_MINUS("NumPad -", 109),
    NUMPAD_DOT("NumPad .", 110),
    NUMPAD_SLASH("NumPad /", 111),
    F1("F1", 112),
    F2("F2", 113),
    F3("F3", 114),
    F4("F4", 115),
    F5("F5", 116),
    F6("F6", 117),
    F7("F7", 118),
    F8("F8", 119),
    F9("F9", 120),
    F10("F10", 121),
    F11("F11", 122),
    F12("F12", 123),
    DELETE("Delete", 127);

    companion object {

        @JvmStatic internal val ENTRIES = values()
        @JvmStatic internal val MAPPED_ENTRIES = ENTRIES.associateBy { it.code }
        @JvmStatic val SORTED_KEYS = ENTRIES.sortedBy { it.description }
    }
}
