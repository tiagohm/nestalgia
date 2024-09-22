package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_268

class MMC3Coolboy(console: Console) : MMC3(console) {

    override val registerStartAddress = 0x6000

    override val chrRamSize = 0x40000

    private val exRegs = IntArray(4)

    override fun reset(softReset: Boolean) {
        exRegs.fill(0)
        super.reset(softReset)
        resetMMC3()
        updateState()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        var newPage = page
        val addr = slot * 0x400
        val mask = 0xFF xor (exRegs[0] and 0x80)
        val base = if (chrMode) 0x1000 else 0

        if (exRegs[3].bit4) {
            if (exRegs[3].bit6) {
                when (base xor addr) {
                    0x0400, 0x0C00 -> newPage = newPage and 0x7F
                }
            }

            super.selectChrPage(slot, (newPage and 0x80 and mask) or ((((exRegs[0] and 0x08) shl 4) and mask.inv())) or ((exRegs[2] and 0x0F) shl 3) or slot, memoryType)
        } else {
            if (exRegs[3].bit6) {
                when (base xor addr) {
                    0x0000 -> newPage = registers[0]
                    0x0800 -> newPage = registers[1]
                    0x0400, 0x0C00 -> newPage = 0
                }
            }

            super.selectChrPage(slot, (newPage and mask) or (((exRegs[0] and 0x08) shl 4) and mask.inv()), memoryType)
        }
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        var newPage = page
        val addr = 0x8000 + slot * 0x2000
        var mask = ((0x3F or (exRegs[1] and 0x40) or ((exRegs[1] and 0x20) shl 2)) xor ((exRegs[0] and 0x40) shr 2)) xor ((exRegs[1] and 0x80) shr 2)
        val base = ((exRegs[0] and 0x07) shr 0) or ((exRegs[1] and 0x10) shr 1) or ((exRegs[1] and 0x0C) shl 2) or ((exRegs[0] and 0x30) shl 2)

        if ((exRegs[3].bit6) && (newPage >= 0xFE) && prgMode) {
            when (slot) {
                1 -> if (prgMode) newPage = 0
                2 -> if (!prgMode) newPage = 0
                3 -> newPage = 0
            }
        }

        if (!exRegs[3].bit4) {
            super.selectPrgPage(slot, (((base shl 4) and mask.inv())) or (newPage and mask), memoryType)
        } else {
            mask = mask and 0xF0

            val emask = if ((((exRegs[1] and 0x02) != 0))) {
                (exRegs[3] and 0x0C) or ((addr and 0x4000) shr 13)
            } else {
                exRegs[3] and 0x0E
            }

            super.selectPrgPage(slot, ((base shl 4) and mask.inv()) or (newPage and mask) or emask or (slot and 0x01), memoryType)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (state.regA001.bit7) {
                writePrgRam(addr, value)
            }

            if ((exRegs[3] and 0x90) != 0x80) {
                exRegs[addr and 0x03] = value
                updateState()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exRegs", exRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("exRegs", exRegs)
    }
}
