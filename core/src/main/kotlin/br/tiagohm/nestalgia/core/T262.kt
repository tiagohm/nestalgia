package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_265

class T262(console: Console) : Mapper(console) {

    private var locked = false
    private var base = 0
    private var bank = 0
    private var mode = false

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 7)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (!locked) {
            base = (addr and 0x60 shr 2) or (addr and 0x100 shr 3)
            mode = addr.bit7
            locked = addr and 0x2000 == 0x2000
            mirroringType = if (addr.bit1) HORIZONTAL else VERTICAL
        }

        bank = value and 0x07

        selectPrgPage(0, base or bank)
        selectPrgPage(1, base or if (mode) bank else 7)
    }
}
