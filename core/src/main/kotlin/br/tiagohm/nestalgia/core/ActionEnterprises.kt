package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*


// https://wiki.nesdev.com/w/index.php/INES_Mapper_228

class ActionEnterprises(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        reset()
    }

    override fun reset(softReset: Boolean) {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        var chipSelect = addr shr 11 and 0x03

        if (chipSelect == 3) {
            chipSelect = 2
        }

        val prgPage = addr shr 6 and 0x1F or (chipSelect shl 5)

        if (addr.bit5) {
            selectPrgPage(0, prgPage)
            selectPrgPage(1, prgPage)
        } else {
            selectPrgPage(0, prgPage and 0xFE)
            selectPrgPage(1, (prgPage and 0xFE) + 1)
        }

        selectChrPage(0, addr and 0x0F shl 2 or (value and 0x03))

        mirroringType = if (addr and 0x2000 != 0) HORIZONTAL else VERTICAL
    }
}
