package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_076

class Mapper076 : Namco108() {

    override val chrPageSize = 0x800U

    override fun updateChrMapping() {
        selectChrPage(0U, registers[2].toUShort())
        selectChrPage(1U, registers[3].toUShort())
        selectChrPage(2U, registers[4].toUShort())
        selectChrPage(3U, registers[5].toUShort())
    }
}