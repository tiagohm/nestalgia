package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_213

@ExperimentalUnsignedTypes
class Mapper213 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override fun init() {
        writeRegister(0x8000U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        selectChrPage(0U, (addr shr 3) and 0x07U)
        selectPrgPage(0U, (addr shr 1) and 0x03U)
    }
}