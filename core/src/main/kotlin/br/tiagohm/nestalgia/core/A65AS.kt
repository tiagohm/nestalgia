package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_285

class A65AS(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectChrPage(0, 0)
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (value.bit6) {
            selectPrgPage2x(0, value and 0x1E)
        } else {
            selectPrgPage(0, value and 0x30 shr 1 or (value and 0x07))
            selectPrgPage(1, value and 0x30 shr 1 or 0x07)
        }

        mirroringType = if (value.bit7) {
            if (value.bit5) SCREEN_B_ONLY else SCREEN_A_ONLY
        } else {
            if (value.bit3) HORIZONTAL else VERTICAL
        }
    }
}
