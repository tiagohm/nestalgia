package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_179

@ExperimentalUnsignedTypes
class Henggedianzi179 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x8000U

    override val registerEndAddress: UShort = 0xFFFFU

    override fun init() {
        addRegisterRange(0x5000U, 0x5FFFU, MemoryOperation.WRITE)
        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr >= 0x8000U) {
            mirroringType = if (value.bit0) MirroringType.HORIZONTAL else MirroringType.VERTICAL
        } else {
            selectPrgPage(0U, (value shr 1).toUShort())
        }
    }
}