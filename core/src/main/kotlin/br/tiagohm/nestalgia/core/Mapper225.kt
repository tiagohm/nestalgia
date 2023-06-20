package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_225

class Mapper225(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 1)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val highBit = addr shr 8 and 0x40
        val prgPage = addr shr 6 and 0x3F or highBit

        if (addr and 0x1000 != 0) {
            selectPrgPage(0, prgPage)
            selectPrgPage(1, prgPage)
        } else {
            selectPrgPage(0, prgPage and 0xFE)
            selectPrgPage(1, (prgPage and 0xFE) + 1)
        }

        selectChrPage(0, addr and 0x3F or highBit)

        mirroringType = if (addr and 0x2000 != 0) HORIZONTAL else VERTICAL
    }
}
