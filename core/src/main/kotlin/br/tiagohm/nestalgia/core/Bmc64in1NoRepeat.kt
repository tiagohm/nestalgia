package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_314

class Bmc64in1NoRepeat(console: Console) : Mapper(console) {

    private val regs = IntArray(4)

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        addRegisterRange(0x5000, 0x5003, MemoryAccessType.WRITE)
    }

    override fun reset(softReset: Boolean) {
        regs[0] = 0x80
        regs[1] = 0x43
        regs[2] = 0
        regs[3] = 0

        updateState()
    }

    private fun updateState() {
        if (regs[0].bit7) {
            if (regs[1].bit7) {
                selectPrgPage2x(0, regs[1] and 0x1F shl 1)
            } else {
                val bank = (regs[1] and 0x1F shl 1) or (regs[1] shr 6 and 0x01)
                selectPrgPage(0, bank)
                selectPrgPage(1, bank)
            }
        } else {
            selectPrgPage(1, (regs[1] and 0x1F shl 1) or (regs[1] shr 6 and 0x01))
        }

        mirroringType = if (regs[0].bit5) HORIZONTAL else VERTICAL
        selectChrPage(0, (regs[2] shl 2) or (regs[0] shr 1 and 0x03))
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            regs[addr and 0x03] = value
        } else {
            regs[3] = value
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

