package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_148

class Sachen148 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val hasBusConflicts = true

    override fun init() {
        selectPrgPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        selectPrgPage(0U, (value.toUShort() shr 3) and 0x01U)
        selectChrPage(0U, value.toUShort() and 0x07U)
    }
}