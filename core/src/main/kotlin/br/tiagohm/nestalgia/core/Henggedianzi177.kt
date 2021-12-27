package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_177

class Henggedianzi177 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x8000U

    override val registerEndAddress: UShort = 0xFFFFU

    override fun init() {
        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        selectPrgPage(0U, value.toUShort())
        mirroringType = if (value.bit5) MirroringType.HORIZONTAL else MirroringType.VERTICAL
    }
}