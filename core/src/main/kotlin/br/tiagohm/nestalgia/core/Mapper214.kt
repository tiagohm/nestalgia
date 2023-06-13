package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_214

class Mapper214(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectChrPage(0, addr and 0x03)
        val page = addr shr 2 and 0x03
        selectPrgPage(0, page)
        selectPrgPage(1, page)
    }
}
