package br.tiagohm.nestalgia.core

class FlashSST39SF040(private val data: IntArray) : Snapshotable, Resetable, Writable, Readable {

    private enum class ChipMode {
        WAITING,
        WRITE,
        ERASE,
    }

    @Volatile private var mode = ChipMode.WAITING
    @Volatile private var cycle = 0
    @Volatile private var softwareId = false

    override fun saveState(s: Snapshot) {
        s.write("mode", mode)
        s.write("cycle", cycle)
        s.write("softwareId", softwareId)
    }

    override fun restoreState(s: Snapshot) {
        mode = s.readEnum("mode", ChipMode.WAITING)
        cycle = s.readInt("cycle")
        softwareId = s.readBoolean("softwareId")
    }

    override fun read(addr: Int): Int {
        return if (softwareId) when (addr and 0x1FF) {
            0x00 -> 0xBF
            0x01 -> 0xB7
            else -> 0xFF
        } else -1
    }

    override fun write(addr: Int, value: Int) {
        val cmd = addr and 0x7FFF

        if (mode == ChipMode.WAITING) {
            if (cycle == 0) {
                if (cmd == 0x5555 && value == 0xAA) {
                    // 1st write, $5555 = $AA.
                    cycle++
                } else if (value == 0xF0) {
                    // Software ID exit.
                    reset()
                    softwareId = false
                }
            } else if (cycle == 1 && cmd == 0x2AAA && value == 0x55) {
                // 2nd write, $2AAA = $55.
                cycle++
            } else if (cycle == 2 && cmd == 0x5555) {
                // 3rd write, determines command type.
                cycle++

                when (value) {
                    0x80 -> mode = ChipMode.ERASE
                    0x90 -> {
                        reset()
                        softwareId = true
                    }
                    0xA0 -> mode = ChipMode.WRITE
                    0xF0 -> {
                        reset()
                        softwareId = false
                    }
                }
            } else {
                cycle = 0
            }
        } else if (mode == ChipMode.WRITE) {
            // Write a single byte.
            if (addr < data.size) {
                data[addr] = data[addr] and value
            }

            reset()
        } else if (mode == ChipMode.ERASE) {
            if (cycle == 3) {
                // 4th write for erase command, $5555 = $AA.
                if (cmd == 0x5555 && value == 0xAA) {
                    cycle++
                } else {
                    reset()
                }
            } else if (cycle == 4) {
                // 5th write for erase command, $2AAA = $55.
                if (cmd == 0x2AAA && value == 0x55) {
                    cycle++
                } else {
                    reset()
                }
            } else if (cycle == 5) {
                if (cmd == 0x5555 && value == 0x10) {
                    // Chip erase.
                    data.fill(0xFF)
                } else if (value == 0x30) {
                    // Sector erase.
                    val offset = addr and 0x7F000

                    if (offset + 0x1000 <= data.size) {
                        data.fill(0xFF, offset, offset + 0x1000)
                    }
                }

                reset()
            }
        }
    }

    override fun reset(softReset: Boolean) {
        mode = ChipMode.WAITING
        cycle = 0
    }
}
