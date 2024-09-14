package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_120

class Mapper120(console: Console) : Mapper(console) {

    @Volatile private var prgReg = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x41FF

    override val registerEndAddress = 0x41FF

    override fun initialize() {
        updatePrg()
        selectPrgPage4x(0, 8)
        selectChrPage(0, 0)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updatePrg() {
        addCpuMemoryMapping(0x6000, 0x7FFF, prgReg, ROM)
    }

    override fun writeRegister(addr: Int, value: Int) {
        prgReg = value
        updatePrg()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg")

        updatePrg()
    }
}
