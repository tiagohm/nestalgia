package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_086

class JalecoJF16 : Mapper() {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val hasBusConflicts = true

    override fun initialize() {
        selectPrgPage(0, powerOnByte())
        selectPrgPage(1, -1)

        selectChrPage(0, powerOnByte())
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value and 0x07)
        selectChrPage(0, value shr 4 and 0x0F)

        mirroringType = if (info.subMapperId == 3) {
            // 078: 3 Holy Diver.
            if (value.bit3) VERTICAL else HORIZONTAL
        } else {
            if (value.bit3) SCREEN_B_ONLY else SCREEN_A_ONLY
        }
    }
}
