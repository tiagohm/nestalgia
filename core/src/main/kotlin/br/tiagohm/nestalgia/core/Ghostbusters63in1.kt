package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

class Ghostbusters63in1(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    private var regs = IntArray(2)

    override fun initialize() = Unit

    override fun reset(softReset: Boolean) {
        regs.fill(0)
        updateState()
    }

    private fun updateState() {
        val chip = regs[1] shl 5 and 0x20 shl (regs[0] shr 7)

        if (chip < regs[0] shr 7) {
            removeCpuMemoryMapping(0x8000, 0xFFFF)
        } else {
            selectPrgPage(0, chip or (regs[0] and 0x1E) or (regs[0] shr 5 and regs[0]))
            selectPrgPage(1, chip or (regs[0] and 0x1F) or (regs[0].inv() shr 5 and 0x01))
        }

        selectChrPage(0, 0)
        mirroringType = if (regs[0].bit6) VERTICAL else HORIZONTAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        regs[addr and 0x01] = value
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)

        updateState()
    }
}
