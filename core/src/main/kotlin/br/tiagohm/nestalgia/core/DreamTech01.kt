package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_521

class DreamTech01(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x5020

    override val registerEndAddress = 0x5020

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 8)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value and 0x07)
    }
}
