package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_133

class Sachen133(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x4100

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x6100 == 0x4100) {
            selectPrgPage(0, value shr 2 and 0x01)
            selectChrPage(0, value and 0x03)
        }
    }
}
