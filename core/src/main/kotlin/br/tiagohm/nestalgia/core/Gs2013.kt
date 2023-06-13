package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.PrgMemoryType.*

class Gs2013(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectChrPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        addCpuMemoryMapping(0x6000, 0x7FFF, 0x1F, ROM)
        selectPrgPage4x(0, 0x0F shl 2)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage4x(0, value and 0x0F shl 2)
    }
}
