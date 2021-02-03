package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_145

@ExperimentalUnsignedTypes
class Sachen145 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x4100U

    override val registerEndAddress: UShort = 0x7FFFU

    override fun init() {
        selectPrgPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr.toInt() and 0x4100 == 0x4100) {
            selectChrPage(0U, (value.toUShort() shr 7) and 0x01U)
        }
    }
}