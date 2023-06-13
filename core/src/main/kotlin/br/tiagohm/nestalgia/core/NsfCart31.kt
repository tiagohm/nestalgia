package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_031

class NsfCart31(console: Console) : Mapper(console) {

    override val prgPageSize = 0x1000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x5000

    override val registerEndAddress = 0x5FFF

    override fun initialize() {
        writeRegister(0x5FFF, 0xFF)

        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(addr and 0x07, value)
    }
}
