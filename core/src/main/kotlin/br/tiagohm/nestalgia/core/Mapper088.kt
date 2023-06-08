package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_088

open class Mapper088 : Namco108() {

    override fun updateChrMapping() {
        registers[0] = registers[0] and 0x3F
        registers[1] = registers[1] and 0x3F

        registers[2] = registers[2] or 0x40
        registers[3] = registers[3] or 0x40
        registers[4] = registers[4] or 0x40
        registers[5] = registers[5] or 0x40

        super.updateChrMapping()
    }
}
