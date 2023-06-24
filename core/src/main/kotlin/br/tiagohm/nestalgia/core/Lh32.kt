package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_125

class Lh32(console: Console) : Mapper(console) {

    private var prgReg = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0x6000

    override fun initialize() {
        selectChrPage(0, 0)
        selectPrgPage(0, -4)
        selectPrgPage(1, -3)
        selectPrgPage(2, 0, WRAM)
        selectPrgPage(3, -1)

        updateState()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateState() {
        addCpuMemoryMapping(0x6000, 0x7FFF, prgReg, ROM)
    }

    override fun writeRegister(addr: Int, value: Int) {
        prgReg = value
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg")

        updateState()
    }
}
