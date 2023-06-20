package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_229

class Mapper229(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectChrPage(0, addr and 0xFF)

        if (addr and 0x1E == 0) {
            selectPrgPage2x(0, 0)
        } else {
            selectPrgPage(0, addr and 0x1F)
            selectPrgPage(1, addr and 0x1F)
        }

        mirroringType = if (value.bit5) HORIZONTAL else VERTICAL
    }
}
