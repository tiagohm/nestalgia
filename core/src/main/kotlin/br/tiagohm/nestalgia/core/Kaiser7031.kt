package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_305

class Kaiser7031(console: Console) : Mapper(console) {

    private val regs = IntArray(4)

    override val prgPageSize = 0x800

    override val chrPageSize = 0x2000

    override fun initialize() {
        mirroringType = VERTICAL

        repeat(16) {
            selectPrgPage(it, 15 - it)
        }

        selectChrPage(0, 0)
        updateState()
    }

    private fun updateState() {
        repeat(4) {
            addCpuMemoryMapping(0x6000 + it * 0x800, 0x67FF + it * 0x800, regs[it], PrgMemoryType.ROM)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        regs[addr shr 11 and 0x03] = value
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
