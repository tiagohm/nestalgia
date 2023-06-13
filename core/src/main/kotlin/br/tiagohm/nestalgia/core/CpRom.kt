package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_013

class CpRom(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x1000

    override val chrRamSize = 0x4000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)

        mirroringType = VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr >= 0x8000) {
            selectChrPage(1, value and 0x03)
        }
    }
}
