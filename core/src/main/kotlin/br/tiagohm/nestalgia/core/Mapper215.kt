package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_215

class Mapper215(console: Console) : MMC3(console) {

    private val exRegs = intArrayOf(0, 3, 0)

    override val registerStartAddress = 0x5000

    override val registerEndAddress = 0xFFFF

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (exRegs[0].bit6) {
            super.selectChrPage(slot, exRegs[1] and 0x0C shl 6 or (page and 0x7F) or (exRegs[1] and 0x20 shl 2), memoryType)
        } else {
            super.selectChrPage(slot, exRegs[1] and 0x0C shl 6 or page, memoryType)
        }
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        var sbank = 0
        var bank = 0

        val mask = if (exRegs[0].bit6) {
            sbank = exRegs[1] and 0x10

            if (exRegs[0].bit7) {
                bank = exRegs[1] and 0x03 shl 4 or (exRegs[0] and 0x07) or (sbank shr 1)
            }

            0x0F
        } else {
            if (exRegs[0].bit7) {
                bank = exRegs[1] and 0x03 shl 4 or (exRegs[0] and 0x0F)
            }

            0x1F
        }

        if (exRegs[0].bit7) {
            bank = bank shl 1

            if (exRegs[0].bit5) {
                super.selectPrgPage(0, bank, memoryType)
                super.selectPrgPage(1, bank + 1, memoryType)
                super.selectPrgPage(2, bank + 2, memoryType)
                super.selectPrgPage(3, bank + 3, memoryType)
            } else {
                super.selectPrgPage(0, bank, memoryType)
                super.selectPrgPage(1, bank + 1, memoryType)
                super.selectPrgPage(2, bank, memoryType)
                super.selectPrgPage(3, bank + 1, memoryType)
            }
        } else {
            super.selectPrgPage(slot, exRegs[1] and 0x03 shl 5 or (page and mask) or sbank, memoryType)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            when (addr) {
                0x5000 -> {
                    exRegs[0] = value
                    updateState()
                }
                0x5001 -> {
                    exRegs[1] = value
                    updateState()
                }
                0x5007 -> exRegs[2] = value
            }
        } else {
            val lutValue = LUT_ADDR[exRegs[2]][addr shr 12 and 0x06 or (addr and 0x01)]
            val newAddr = lutValue and 0x01 or (lutValue and 0x06 shl 12) or 0x8000

            super.writeRegister(newAddr, if (lutValue == 0) value and 0xC0 or LUT_REG[exRegs[2]][value and 0x07] else value)
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

    companion object {

        @JvmStatic private val LUT_REG = arrayOf(
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            intArrayOf(0, 2, 6, 1, 7, 3, 4, 5),
            intArrayOf(0, 5, 4, 1, 7, 2, 6, 3),
            intArrayOf(0, 6, 3, 7, 5, 2, 4, 1),
            intArrayOf(0, 2, 5, 3, 6, 1, 7, 4),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
        )

        @JvmStatic private val LUT_ADDR = arrayOf(
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            intArrayOf(3, 2, 0, 4, 1, 5, 6, 7),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            intArrayOf(5, 0, 1, 2, 3, 7, 6, 4),
            intArrayOf(3, 1, 0, 5, 2, 4, 6, 7),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
        )
    }
}
