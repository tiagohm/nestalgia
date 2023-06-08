package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_246

class Mapper246 : Mapper() {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x800

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0x67FF

    override fun initialize() {
        selectPrgPage(3, 0xFF)
    }

    override fun reset(softReset: Boolean) {
        selectPrgPage(3, 0xFF)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x7 <= 0x3) {
            selectPrgPage(addr and 0x03, value)
        } else {
            selectChrPage(addr and 0x03, value)
        }
    }
}
