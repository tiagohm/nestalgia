package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.PARTY_TAP

// https://www.nesdev.org/wiki/Partytap

class PartyTap(console: Console, private val keyMapping: KeyMapping) : ControlDevice(console, PARTY_TAP, EXP_DEVICE_PORT) {

    enum class Button(override val bit: Int) : ControllerButton, HasCustomKey {
        B1(0),
        B2(1),
        B3(2),
        B4(3),
        B5(4),
        B6(5);

        override val keyIndex = 32 + ordinal
    }

    @Volatile private var stateBuffer = 0
    @Volatile private var readCount = 0
    @Volatile private var enabled = false

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
        s.write("enabled", enabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        stateBuffer = s.readInt("stateBuffer")
        readCount = s.readInt("readCount")
        enabled = s.readBoolean("enabled")
    }
}
