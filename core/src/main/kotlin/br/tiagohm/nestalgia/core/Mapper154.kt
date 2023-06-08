package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_154

class Mapper154 : Mapper088() {

    override fun writeRegister(addr: Int, value: Int) {
        mirroringType = if (value.bit6) SCREEN_B_ONLY else SCREEN_A_ONLY
        super.writeRegister(addr, value)
    }
}
