package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_149

class Sachen149 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override fun init() {
        selectPrgPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        selectChrPage(0U, (value.toUShort() shr 7) and 0x01U)
    }
}