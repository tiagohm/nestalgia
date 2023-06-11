package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_097

class IremTamS1 : Mapper() {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, -1)
        selectPrgPage(1, -1)

        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(1, value and 0x0F)

        mirroringType = when (value shr 6) {
            0 -> SCREEN_A_ONLY
            1 -> HORIZONTAL
            2 -> VERTICAL
            3 -> SCREEN_B_ONLY
            else -> return
        }
    }
}
