package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_089

class Sunsoft89(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(1, -1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value shr 4 and 0x07)
        selectChrPage(0, value and 0x07 or (value and 0x80 shr 4))
        mirroringType = if (value.bit3) SCREEN_B_ONLY else SCREEN_A_ONLY
    }
}
