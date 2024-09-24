package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE
import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_403

class Mapper403(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    private val regs = IntArray(4)

    override fun initialize() {
        addRegisterRange(0x4100, 0x5FFF, WRITE)

        // TODO: Hack for TetrisA from Tetris Family 19-in-1 NO 1683 to work.
        addRegisterRange(0x6000, 0x7FFF, READ)

        updateState()
    }

    override fun reset(softReset: Boolean) {
        regs.fill(0)
        updateState()
    }

    private fun updateState() {
        if (regs[2].bit0) {
            selectPrgPage(0, regs[0] shr 1 and 0x3F)
            selectPrgPage(1, regs[0] shr 1 and 0x3F)
        } else {
            selectPrgPage2x(0, regs[0] shr 1 and 0x3E)
        }

        selectChrPage(0, regs[1] and 0x03)

        mirroringType = if (regs[2].bit4) HORIZONTAL else VERTICAL
    }

    override fun readRegister(addr: Int): Int {
        return internalRead(addr)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (addr and 0x0100 != 0) {
                regs[addr and 0x03] = value
                updateState()
            }
        } else {
            if (regs[2].bit2) {
                regs[1] = value
                updateState()
            }
        }
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
