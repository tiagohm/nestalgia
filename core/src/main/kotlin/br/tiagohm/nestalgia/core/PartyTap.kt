package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.PARTY_TAP

// https://www.nesdev.org/wiki/Partytap

class PartyTap(console: Console, private val keyMapping: KeyMapping) : ControlDevice(console, PARTY_TAP, EXP_DEVICE_PORT) {

    enum class Button : ControllerButton, HasCustomKey {
        B1,
        B2,
        B3,
        B4,
        B5,
        B6;

        override val bit = ordinal

        override val keyIndex = 32 + ordinal
    }

    @Volatile private var stateBuffer = 0
    @Volatile private var readCount = 0

    private val keys = Button.entries.map(keyMapping::customKey).toTypedArray()

    override fun setStateFromInput() {
        Button.entries.forEach { setPressedState(it, keys[it.ordinal]) }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        if (addr == 0x4017) {
            strobeOnRead()

            if (readCount < 2) {
                val value = stateBuffer and 0x7 shl 2
                stateBuffer = stateBuffer shr 3
                readCount++
                return value
            } else {
                // After 1st/2nd reads,	a detection value can be read : $4017 & $1C == $14
                return 0x14
            }
        }

        return 0
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobeOnWrite(value)
    }

    override fun refreshStateBuffer() {
        readCount = 0
        stateBuffer = (if (isPressed(Button.B1)) 1 else 0) or
            (if (isPressed(Button.B2)) 2 else 0) or
            (if (isPressed(Button.B3)) 4 else 0) or
            (if (isPressed(Button.B4)) 8 else 0) or
            (if (isPressed(Button.B5)) 16 else 0) or
            (if (isPressed(Button.B6)) 32 else 0)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("stateBuffer", stateBuffer)
        s.write("readCount", readCount)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        stateBuffer = s.readInt("stateBuffer")
        readCount = s.readInt("readCount")
    }

    companion object : HasDefaultKeyMapping {

        override fun populateWithDefault(keyMapping: KeyMapping) {
            keyMapping.customKey(Button.B1, KeyboardKeys.NUMBER_1)
            keyMapping.customKey(Button.B2, KeyboardKeys.NUMBER_2)
            keyMapping.customKey(Button.B3, KeyboardKeys.NUMBER_3)
            keyMapping.customKey(Button.B4, KeyboardKeys.NUMBER_4)
            keyMapping.customKey(Button.B5, KeyboardKeys.NUMBER_5)
            keyMapping.customKey(Button.B6, KeyboardKeys.NUMBER_6)
        }
    }
}
