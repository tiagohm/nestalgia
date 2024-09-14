package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.FAMILY_TRAINER_MAT_SIDE_B
import br.tiagohm.nestalgia.core.ControllerType.POWER_PAD_SIDE_B

// https://www.nesdev.org/wiki/Power_Pad

open class PowerPad(
    console: Console, type: ControllerType, port: Int,
    private val keyMapping: KeyMapping,
) : ControlDevice(console, type, port) {

    enum class Button(override val bit: Int) : ControllerButton, HasCustomKey {
        B01(0),
        B02(1),
        B03(2),
        B04(3),
        B05(4),
        B06(5),
        B07(6),
        B08(7),
        B09(8),
        B10(9),
        B11(10),
        B12(11);

        override val keyIndex = 18 + ordinal
    }

    private val isSideB = type == POWER_PAD_SIDE_B || type == FAMILY_TRAINER_MAT_SIDE_B
    private val keys = Array(12) { keyMapping.customKey(Button.entries[it]) }

    @Volatile private var stateBufferL = 0
    @Volatile private var stateBufferH = 0

    override fun setStateFromInput() {
        for (i in 0..2) {
            for (j in 0..3) {
                val index = i * 4 + j

                if (isSideB) {
                    // Invert the order of each row.
                    setPressedState(Button.entries[index], keys[i * 4 + (3 - j)])
                } else {
                    setPressedState(Button.entries[index], keys[index])
                }
            }
        }
    }

    override fun refreshStateBuffer() {
        // Serial data from buttons 2, 1, 5, 9, 6, 10, 11, 7.
        stateBufferL = (if (isPressed(1)) 0x01 else 0) or
            (if (isPressed(0)) 0x02 else 0) or
            (if (isPressed(4)) 0x04 else 0) or
            (if (isPressed(8)) 0x08 else 0) or
            (if (isPressed(5)) 0x10 else 0) or
            (if (isPressed(9)) 0x20 else 0) or
            (if (isPressed(10)) 0x40 else 0) or
            (if (isPressed(6)) 0x80 else 0)

        // Serial data from buttons 4, 3, 12, 8 (following 4 bits read as H=1).
        stateBufferH = (if (isPressed(3)) 0x01 else 0) or
            (if (isPressed(2)) 0x02 else 0) or
            (if (isPressed(11)) 0x04 else 0) or
            (if (isPressed(7)) 0x08 else 0) or 0xF0

    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        var output = 0

        if (isCurrentPort(addr)) {
            strobeOnRead()

            output = (stateBufferH and 0x01 shl 4) or (stateBufferL and 0x01 shl 3)
            stateBufferL = stateBufferL shr 1
            stateBufferH = stateBufferH shr 1
            stateBufferL = stateBufferL or 0x80
            stateBufferH = stateBufferH or 0x80
        }

        return output
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobeOnWrite(value)
    }
}
