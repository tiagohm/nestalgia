package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_077

class IremLrog017(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x0800

    override val chrRamSize = 0x1800

    override val chrRamPageSize = 0x0800

    override val hasBusConflicts = true

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)

        mirroringType = FOUR_SCREENS

        selectChrPage(1, 0, RAM)
        selectChrPage(2, 1, RAM)
        selectChrPage(3, 2, RAM)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value and 0x0F)
        selectChrPage(0, value shr 4 and 0x0F)
    }
}
