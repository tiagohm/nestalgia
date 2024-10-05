package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_385

class Mapper385(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val page = addr shr 1

        selectPrgPage(0, page)
        selectPrgPage(1, page)

        mirroringType = if (addr.bit0) HORIZONTAL else VERTICAL
    }
}
