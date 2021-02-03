package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_242

@ExperimentalUnsignedTypes
class Mapper242 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override fun init() {
        reset(false)
        selectChrPage(0U, 0U)
    }

    override fun reset(softReset: Boolean) {
        selectPrgPage(0U, 0U)
        mirroringType = MirroringType.VERTICAL
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        mirroringType = if (addr.bit1) MirroringType.HORIZONTAL else MirroringType.VERTICAL
        selectPrgPage(0U, (addr shr 3) and 0x0FU)
    }
}