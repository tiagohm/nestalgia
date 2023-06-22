package br.tiagohm.nestalgia.core

class FamilyTrainerMat(console: Console, type: ControllerType, keyMapping: KeyMapping) : PowerPad(console, type, EXP_DEVICE_PORT, keyMapping) {

    private var ignoreRows = 0

    override fun refreshStateBuffer() {}

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr == 0x4017) {
            val pressedKeys = IntArray(4)

            for (j in 0..2) {
                if ((ignoreRows shr 2 - j).bit0) {
                    // Ignore this row.
                    continue
                }

                for (i in 0..3) {
                    pressedKeys[i] = pressedKeys[i] or if (isPressed(j * 4 + i)) 1 else 0
                }
            }

            (pressedKeys[0] shl 4 or
                (pressedKeys[1] shl 3) or
                (pressedKeys[2] shl 2) or
                (pressedKeys[3] shl 1)).inv() and 0x1E
        } else {
            0
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        ignoreRows = value and 0x07
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("ignoreRows", ignoreRows)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        ignoreRows = s.readInt("ignoreRows")
    }
}
