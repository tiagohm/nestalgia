package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_214

class Mapper214 : Mapper() {

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x2000U

    override fun init() {
        writeRegister(0x8000U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        selectChrPage(0U, addr and 0x03U)
        val page = (addr shr 2) and 0x03U
        selectPrgPage(0U, page)
        selectPrgPage(1U, page)
    }
}