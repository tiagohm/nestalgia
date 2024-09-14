package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_062

class Mapper062(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        reset(true)
    }

    override fun reset(softReset: Boolean) {
        if (softReset) {
            selectPrgPage(0, 0)
            selectPrgPage(1, 1)
            selectChrPage(0, 0)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        val prgPage = (addr and 0x3F00 shr 8) or (addr and 0x40)
        val chrPage = (addr and 0x1F shl 2) or (value and 0x03)

        if (addr.bit5) {
            selectPrgPage(0, prgPage)
            selectPrgPage(1, prgPage)
        } else {
            selectPrgPage(0, prgPage and 0xFE)
            selectPrgPage(1, (prgPage and 0xFE) + 1)
        }

        selectChrPage(0, chrPage)

        mirroringType = if (addr.bit7) HORIZONTAL else VERTICAL
    }
}
