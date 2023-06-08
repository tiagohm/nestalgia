package br.tiagohm.nestalgia.core

class Nina01 : Mapper() {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x1000

    override val registerStartAddress = 0x7FFD

    override val registerEndAddress = 0x7FFF

    override fun initialize() {
        selectPrgPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0x7FFD -> selectPrgPage(0, value and 0x01)
            0x7FFE -> selectChrPage(0, value and 0x0F)
            0x7FFF -> selectChrPage(1, value and 0x0F)
        }

        writePrgRam(addr, value)
    }
}
