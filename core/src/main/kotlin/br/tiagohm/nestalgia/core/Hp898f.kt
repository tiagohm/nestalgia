package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_319

class Hp898f(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    private val regs = IntArray(2)

    override fun initialize() {
        updateState()
    }

    private fun updateState() {
        val prgReg = regs[1] shr 3 and 7
        val prgMask = regs[1] shr 4 and 4
        selectChrPage(0, ((regs[0] shr 4 and 0x07) and ((regs[0] and 0x01 shl 2) or (regs[0] and 0x02)).inv()))
        selectPrgPage(0, prgReg and prgMask.inv())
        selectPrgPage(1, prgReg or prgMask)
        mirroringType = if (regs[1].bit7) VERTICAL else HORIZONTAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x6000 != 0) {
            regs[addr and 0x04 shr 2] = value
            updateState()
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
