package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_002

class UNROM(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val hasBusConflicts
        get() = info.subMapperId == 2

    override fun initialize() {
        // First and last PRG page
        selectPrgPage(0, 0)
        selectPrgPage(1, -1)

        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        // Select 16 KB PRG ROM bank for CPU $8000-$BFFF
        selectPrgPage(0, value)
    }
}
