package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_076

class Mapper076 : Namco108() {

    override val chrPageSize = 0x800

    override fun updateChrMapping() {
        selectChrPage(0, registers[2])
        selectChrPage(1, registers[3])
        selectChrPage(2, registers[4])
        selectChrPage(3, registers[5])
    }
}
