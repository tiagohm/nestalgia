package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_241

class Mapper241 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override fun init() {
        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        selectPrgPage(0U, value.toUShort())
    }
}