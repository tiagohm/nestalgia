package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_066

class GxROM(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, powerOnByte() and 0x03)
        selectChrPage(0, powerOnByte() and 0x03)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value shr 4 and 0x03)
        selectChrPage(0, value and 0x03)
    }
}
