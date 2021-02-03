package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_240

@ExperimentalUnsignedTypes
class Mapper240 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x4020U

    override val registerEndAddress: UShort = 0x5FFFU

    override fun init() {
        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        selectPrgPage(0U, ((value shr 4) and 0x0FU).toUShort())
        selectChrPage(0U, value.toUShort() and 0x0FU)
    }
}