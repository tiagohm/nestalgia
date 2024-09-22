package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_319

class Mapper319(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    private val regs = IntArray(3)
    @Volatile private var unifBankOrder = false

    override fun initialize() {
        if (data.info.hash.prgCrc32 == 0xC25FD362) {
            unifBankOrder = true
        }

        updateState()
    }

    private fun updateState() {
        if (unifBankOrder) {
            // The publicly-available UNIF ROM file of Prima Soft 9999999-in-1 has the order of the 16 KiB PRG-ROM banks
            // slightly mixed up, so that the PRG A14 mode bit operates on A16 instead of A14.
            // To obtain the correct bank order,
            // use UNIF 16 KiB PRG banks 0, 4, 1, 5, 2, 6, 3, 7.
            val prgReg = regs[1] shr 3 and 7
            val prgMask = regs[1] shr 4 and 4

            selectChrPage(0, ((regs[0] shr 4 and 0x07) and ((regs[0] and 0x01 shl 2) or (regs[0] and 0x02)).inv()))
            selectPrgPage(0, prgReg and (prgMask.inv()))
            selectPrgPage(1, prgReg or prgMask)
        } else {
            if (regs[1].bit6) {
                selectPrgPage2x(0, regs[1] shr 2 and 0xE)
            } else {
                val bank = (regs[1] shr 2 and 0x06) or (regs[1] shr 5 and 0x01)
                selectPrgPage(0, bank)
                selectPrgPage(1, bank)
            }

            selectChrPage(0, ((regs[0] shr 4) and (regs[0] shl 2 and 0x04).inv()) or (regs[2] shl 2 and (regs[0] shl 2 and 0x04)))
        }

        mirroringType = if (regs[1].bit7) MirroringType.VERTICAL else MirroringType.HORIZONTAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            regs[addr and 0x04 shr 2] = value
        } else {
            regs[2] = value
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
    }
}
