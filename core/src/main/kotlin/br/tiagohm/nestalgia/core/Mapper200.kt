package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_200

class Mapper200(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        bank(0)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun bank(bank: Int) {
        selectPrgPage(0, bank)
        selectPrgPage(1, bank)
        selectChrPage(0, bank)
    }

    override fun writeRegister(addr: Int, value: Int) {
        bank(addr and 0x07)

        mirroringType = if (addr.bit3) HORIZONTAL else VERTICAL
    }
}
