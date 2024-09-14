package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.SCREEN_A_ONLY
import br.tiagohm.nestalgia.core.MirroringType.SCREEN_B_ONLY

// https://wiki.nesdev.com/w/index.php/INES_Mapper_154

class Mapper154(console: Console) : Mapper088(console) {

    override fun writeRegister(addr: Int, value: Int) {
        mirroringType = if (value.bit6) SCREEN_B_ONLY else SCREEN_A_ONLY
        super.writeRegister(addr, value)
    }
}
