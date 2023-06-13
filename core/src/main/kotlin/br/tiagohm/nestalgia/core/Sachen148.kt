package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_148

class Sachen148(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val hasBusConflicts = true

    override fun initialize() {
        selectPrgPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value shr 3 and 0x01)
        selectChrPage(0, value and 0x07)
    }
}
