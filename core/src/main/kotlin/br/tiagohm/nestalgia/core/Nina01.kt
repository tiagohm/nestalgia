package br.tiagohm.nestalgia.core

class Nina01 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x1000U

    override val registerStartAddress: UShort = 0x7FFDU

    override val registerEndAddress: UShort = 0x7FFFU

    override fun init() {
        selectPrgPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        when (addr.toInt()) {
            0x7FFD -> selectPrgPage(0U, value.toUShort() and 0x01U)
            0x7FFE -> selectChrPage(0U, value.toUShort() and 0x0FU)
            0x7FFF -> selectChrPage(1U, value.toUShort() and 0x0FU)
        }

        writePrgRam(addr, value)
    }
}