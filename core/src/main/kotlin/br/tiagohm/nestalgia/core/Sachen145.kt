package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_145

class Sachen145(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x4100

    override val registerEndAddress = 0x7FFF

    override fun initialize() {
        selectPrgPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x4100 == 0x4100) {
            selectChrPage(0, value shr 7 and 0x01)
        }
    }
}
