package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_088

@ExperimentalUnsignedTypes
class Mapper088 : Namco108() {

    override fun updateChrMapping() {
        registers[0] = registers[0] and 0x3FU
        registers[1] = registers[1] and 0x3FU

        registers[2] = registers[2] or 0x40U
        registers[3] = registers[3] or 0x40U
        registers[4] = registers[4] or 0x40U
        registers[5] = registers[5] or 0x40U

        super.updateChrMapping()
    }
}