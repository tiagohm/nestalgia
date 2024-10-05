package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_437

class Mapper437(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    private val regs = IntArray(2)

    override fun initialize() {
        addRegisterRange(0x5000, 0x5FFF, MemoryAccessType.WRITE)
        reset()
    }

    override fun reset(softReset: Boolean) {
        writeRegister(0x5000, 0)
        writeRegister(0x8000, 0)
    }

    private fun updateState() {
        selectChrPage(0, 0)
        selectPrgPage(0, regs[0] shl 3 or (regs[1] and 0x07))
        selectPrgPage(1, regs[0] shl 3 or 0x07)
        mirroringType = if (regs[0].bit3) HORIZONTAL else VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            regs[0] = addr and 0x0F
        } else {
            regs[1] = value
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
