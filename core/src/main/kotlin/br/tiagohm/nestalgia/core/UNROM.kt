package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_002

@ExperimentalUnsignedTypes
class UNROM : Mapper() {

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x2000U

    override val hasBusConflicts: Boolean
        get() = info.subMapperId == 2

    override fun init() {
        // First and last PRG page
        selectPrgPage(0U, 0U)
        selectPrgPage(1U, 0xFFFFU)

        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        // Select 16 KB PRG ROM bank for CPU $8000-$BFFF
        selectPrgPage(0U, value.toUShort())
    }
}