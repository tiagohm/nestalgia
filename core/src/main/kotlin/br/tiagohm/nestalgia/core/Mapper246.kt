package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_246

class Mapper246 : Mapper() {

    override val prgPageSize = 0x2000U

    override val chrPageSize = 0x800U

    override val registerStartAddress: UShort = 0x6000U

    override val registerEndAddress: UShort = 0x67FFU

    override fun init() {
        selectPrgPage(3U, 0xFFU)
    }

    override fun reset(softReset: Boolean) {
        selectPrgPage(3U, 0xFFU)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr and 0x7U <= 0x3U) {
            selectPrgPage(addr and 0x03U, value.toUShort())
        } else {
            selectChrPage(addr and 0x03U, value.toUShort())
        }
    }
}