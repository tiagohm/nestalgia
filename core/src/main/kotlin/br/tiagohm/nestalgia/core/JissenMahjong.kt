package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.PARTY_TAP

// https://www.nesdev.org/wiki/Jissen_Mahjong_controller

class JissenMahjong(console: Console, private val keyMapping: KeyMapping) : ControlDevice(console, PARTY_TAP, EXP_DEVICE_PORT) {

    enum class Button : ControllerButton, HasCustomKey {
        A, B, C, D, E, F, G, H, I, J, K, L, M, N, SELECT, START, KAN, PON, CHII, RIICHI, RON;

        override val bit = ordinal
        override val keyIndex = 38 + ordinal
    }

    @Volatile private var stateBuffer = 0
    @Volatile private var row = 0

    private val keys = Button.entries.map(keyMapping::customKey).toTypedArray()

    override fun setStateFromInput() {
        Button.entries.forEach { setPressedState(it, keys[it.ordinal]) }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        if (addr == 0x4017) {
            strobeOnRead()

            val value = stateBuffer and 0x01 shl 1
            stateBuffer = stateBuffer shr 1
            return value
        }

        return 0
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        row = value and 0x6 shr 1
        strobeOnWrite(value)
    }

    override fun refreshStateBuffer() {
        stateBuffer = when (row) {
            1 -> (if (isPressed(Button.N)) 0x04 else 0) or
                (if (isPressed(Button.M)) 0x08 else 0) or
                (if (isPressed(Button.L)) 0x10 else 0) or
                (if (isPressed(Button.K)) 0x20 else 0) or
                (if (isPressed(Button.J)) 0x40 else 0) or
                (if (isPressed(Button.I)) 0x80 else 0)
            2 -> (if (isPressed(Button.H)) 0x01 else 0) or
                (if (isPressed(Button.G)) 0x02 else 0) or
                (if (isPressed(Button.F)) 0x04 else 0) or
                (if (isPressed(Button.E)) 0x08 else 0) or
                (if (isPressed(Button.D)) 0x10 else 0) or
                (if (isPressed(Button.C)) 0x20 else 0) or
                (if (isPressed(Button.B)) 0x40 else 0) or
                (if (isPressed(Button.A)) 0x80 else 0)
            3 -> (if (isPressed(Button.RON)) 0x02 else 0) or
                (if (isPressed(Button.RIICHI)) 0x04 else 0) or
                (if (isPressed(Button.CHII)) 0x08 else 0) or
                (if (isPressed(Button.PON)) 0x10 else 0) or
                (if (isPressed(Button.KAN)) 0x20 else 0) or
                (if (isPressed(Button.START)) 0x40 else 0) or
                (if (isPressed(Button.SELECT)) 0x80 else 0)
            else -> 0
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("stateBuffer", stateBuffer)
        s.write("row", row)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        stateBuffer = s.readInt("stateBuffer")
        row = s.readInt("row")
    }

    companion object : HasDefaultKeyMapping {

        override fun populateWithDefault(keyMapping: KeyMapping) {
            keyMapping.customKey(Button.A, KeyboardKeys.A)
            keyMapping.customKey(Button.B, KeyboardKeys.B)
            keyMapping.customKey(Button.C, KeyboardKeys.C)
            keyMapping.customKey(Button.D, KeyboardKeys.D)
            keyMapping.customKey(Button.E, KeyboardKeys.E)
            keyMapping.customKey(Button.F, KeyboardKeys.F)
            keyMapping.customKey(Button.G, KeyboardKeys.G)
            keyMapping.customKey(Button.H, KeyboardKeys.H)
            keyMapping.customKey(Button.I, KeyboardKeys.I)
            keyMapping.customKey(Button.J, KeyboardKeys.J)
            keyMapping.customKey(Button.K, KeyboardKeys.K)
            keyMapping.customKey(Button.L, KeyboardKeys.L)
            keyMapping.customKey(Button.M, KeyboardKeys.M)
            keyMapping.customKey(Button.N, KeyboardKeys.N)
            keyMapping.customKey(Button.SELECT, KeyboardKeys.SPACE)
            keyMapping.customKey(Button.START, KeyboardKeys.ENTER)
            keyMapping.customKey(Button.KAN, KeyboardKeys.NUMBER_1)
            keyMapping.customKey(Button.PON, KeyboardKeys.NUMBER_2)
            keyMapping.customKey(Button.CHII, KeyboardKeys.NUMBER_3)
            keyMapping.customKey(Button.RIICHI, KeyboardKeys.NUMBER_4)
            keyMapping.customKey(Button.RON, KeyboardKeys.NUMBER_5)
        }
    }
}
