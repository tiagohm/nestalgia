package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_081
// https://github.com/Erendel/Mesen/commit/09a2cd01a7e1b417d6cc692598606124a6085bec#diff-e006f33162a2476fa77c7e68864228bd75cdd314e29b97c1f1b6d0e8916f0521R9

class Mapper081(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, -1)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, addr shr 2)
        selectChrPage(0, addr and 0x03)
    }
}
