package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_349

class BmcG146(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() = Unit

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        writeRegister(0x8000, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x800 != 0) {
            selectPrgPage(0, (addr and 0x1F) or (addr and (addr and 0x40 shr 6)))
            selectPrgPage(1, (addr and 0x18) or 0x07)
        } else {
            if (addr.bit6) {
                val page = addr and 0x1F
                selectPrgPage(0, page)
                selectPrgPage(1, page)
            } else {
                selectPrgPage2x(0, addr and 0x1E)
            }
        }

        mirroringType = if (addr.bit7) HORIZONTAL else VERTICAL
    }
}
