package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_174

class Mapper174(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val prgBank = addr shr 4 and 0x07

        if (addr.bit7) {
            selectPrgPage2x(0, prgBank and 0xFE)
        } else {
            selectPrgPage(0, prgBank)
            selectPrgPage(1, prgBank)
        }

        selectChrPage(0, addr shr 1 and 0x07)

        mirroringType = if (addr.bit0) HORIZONTAL else VERTICAL
    }
}
