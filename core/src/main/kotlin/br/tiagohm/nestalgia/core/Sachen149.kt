package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_149

class Sachen149 : Mapper() {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectChrPage(0, value shr 7 and 0x01)
    }
}
