package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_163

class Nanjing(console: Console) : Mapper(console) {

    private val registers = IntArray(5)
    @Volatile private var toggle = true
    @Volatile private var autoSwitchCHR = false

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x1000

    override val registerStartAddress = 0x5000

    override val registerEndAddress = 0x5FFF

    override val allowRegisterRead = true

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
        selectChrPage(1, 0)
    }

    private fun updateState() {
        val prgPage = registers[0] and 0x0F or (registers[2] and 0x0F shl 4)
        autoSwitchCHR = registers[0] and 0x80 == 0x80
        selectPrgPage(0, prgPage)
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (autoSwitchCHR && console.ppu.cycle > 256) {
            if (console.ppu.scanline == 239) {
                selectChrPage(0, 0)
                selectChrPage(1, 0)
            } else if (console.ppu.scanline == 127) {
                selectChrPage(0, 1)
                selectChrPage(1, 1)
            }
        }
    }

    override fun readRegister(addr: Int): Int {
        // Copy protection stuff - based on FCEUX's implementation.
        return when (addr and 0x7700) {
            0x5100 -> registers[3] or registers[1] or registers[0] or (registers[2] xor 0xFF)
            0x5500 -> if (toggle) registers[3] or registers[0] else 0
            else -> 4
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr in 0x5000..0x5FFF) {
            // Address is masked with 0x7300, except for 5101.
            if (addr == 0x5101) {
                if (registers[4] != 0 && value == 0) {
                    // If the value of this register is changed from nonzero to zero,
                    // "trigger" is toggled (XORed with 1)
                    toggle = !toggle
                }

                registers[4] = value
            } else if (addr == 0x5100 && value == 6) {
                selectPrgPage(0, 3)
            } else {
                when (addr and 0x7300) {
                    0x5000 -> {
                        registers[0] = value

                        if (!registers[0].bit7 && console.ppu.scanline < 128) {
                            selectChrPage(0, 0)
                            selectChrPage(1, 1)
                        }

                        updateState()
                    }
                    0x5100 -> {
                        registers[1] = value

                        if (value == 6) {
                            selectPrgPage(0, 3)
                        }
                    }
                    0x5200 -> {
                        registers[2] = value
                        updateState()
                    }
                    0x5300 -> registers[3] = value
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("registers", registers)
        s.write("toggle", toggle)
        s.write("autoSwitchCHR", autoSwitchCHR)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("registers", registers)
        toggle = s.readBoolean("toggle")
        autoSwitchCHR = s.readBoolean("autoSwitchCHR")
    }
}
