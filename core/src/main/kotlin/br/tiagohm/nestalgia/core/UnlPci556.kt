package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_038

class UnlPci556 : Mapper() {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x7000

    override val registerEndAddress = 0x7FFF

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value and 0x03)
        selectChrPage(0, value shr 2 and 0x03)
    }
}
