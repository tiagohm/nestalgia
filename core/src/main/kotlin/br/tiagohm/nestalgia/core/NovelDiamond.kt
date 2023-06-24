package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_054
// https://wiki.nesdev.com/w/index.php/INES_Mapper_201

class NovelDiamond(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, addr and 0x03)
        selectChrPage(0, addr and 0x07)
    }
}
