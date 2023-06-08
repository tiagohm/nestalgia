package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_242

class Mapper242 : Mapper() {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        reset(false)
        selectChrPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        selectPrgPage(0, 0)
        mirroringType = VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        mirroringType = if (addr.bit1) HORIZONTAL
        else VERTICAL

        selectPrgPage(0, addr shr 3 and 0x0F)
    }
}
