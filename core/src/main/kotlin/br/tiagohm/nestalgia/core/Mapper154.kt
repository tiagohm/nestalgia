package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_154

class Mapper154 : Mapper088() {

    override fun writeRegister(addr: UShort, value: UByte) {
        mirroringType = if (value.bit6) MirroringType.SCREEN_B_ONLY else MirroringType.SCREEN_A_ONLY
        super.writeRegister(addr, value)
    }
}