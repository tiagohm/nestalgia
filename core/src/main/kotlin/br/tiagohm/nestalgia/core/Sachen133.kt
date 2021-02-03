package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class Sachen133 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x4100U

    override val registerEndAddress: UShort = 0xFFFFU

    override fun init() {
        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr.toInt() and 0x6100 == 0x4100) {
            selectPrgPage(0U, (value.toUShort() shr 2) and 0x01U)
            selectChrPage(0U, value.toUShort() and 0x03U)
        }
    }
}