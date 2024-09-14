package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_061

class Mapper061(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 1)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val prgPage = addr and 0x0F shl 1 or (addr shr 5 and 0x01)

        if (addr.bit4) {
            selectPrgPage(0, prgPage)
            selectPrgPage(1, prgPage)
        } else {
            selectPrgPage(0, prgPage and 0xFE)
            selectPrgPage(1, (prgPage and 0xFE) + 1)
        }

        mirroringType = if (addr.bit7) HORIZONTAL else VERTICAL

    }
}
