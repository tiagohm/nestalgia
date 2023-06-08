package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_164

class Mapper200 : Mapper() {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        bank(0)
    }

    private fun bank(bank: Int) {
        selectPrgPage(0, bank)
        selectPrgPage(1, bank)
        selectChrPage(0, bank)
    }

    override fun writeRegister(addr: Int, value: Int) {
        bank(addr and 0x07)

        mirroringType = if (value.bit3) VERTICAL
        else HORIZONTAL
    }
}
