package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.PrgMemoryType.ROM

class Malee(console: Console) : Mapper(console) {

    override val prgPageSize = 0x800

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage4x(0, 0)
        selectPrgPage4x(1, 4)
        selectPrgPage4x(2, 8)
        selectPrgPage4x(3, 12)

        selectChrPage(0, 0)

        addCpuMemoryMapping(0x6000, 0x67FF, 16, ROM)
    }
}
