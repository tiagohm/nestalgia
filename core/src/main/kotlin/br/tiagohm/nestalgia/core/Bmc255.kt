package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_255

@ExperimentalUnsignedTypes
class Bmc255 : Mapper() {

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x2000U

    override fun init() {
        writeRegister(0x8000U, 0x00U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        val prgBit: UShort = if (addr.toInt() and 0x1000 != 0) 0U else 1U
        val bank = (addr shr 8 and 0x40U) or (addr shr 6 and 0x3FU)

        selectPrgPage(0U, bank and prgBit.inv())
        selectPrgPage(1U, bank or prgBit)
        selectChrPage(0U, (addr shr 8 and 0x40U) or (addr and 0x3FU))
        mirroringType = if (addr.toInt() and 0x2000 != 0) MirroringType.HORIZONTAL else MirroringType.VERTICAL
    }
}