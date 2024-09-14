package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_309

class Lh51(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 13)
        selectPrgPage(2, 14)
        selectPrgPage(3, 15)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE000) {
            0x8000 -> selectPrgPage(0, value and 0x0F)
            0xE000 -> mirroringType = if (value.bit3) HORIZONTAL else VERTICAL
        }
    }
}
