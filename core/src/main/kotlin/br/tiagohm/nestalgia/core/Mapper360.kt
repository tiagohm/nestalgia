package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_360

class Mapper360(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val dipSwitchCount = 5

    override fun initialize() {
        removeRegisterRange(0x8000, 0xFFFF, MemoryAccessType.READ_WRITE)

        val dipsw = dipSwitches

        if (dipsw < 2) {
            selectPrgPage2x(0, dipsw and 0xFE)
        } else {
            selectPrgPage(0, dipsw)
            selectPrgPage(1, dipsw)
        }

        selectChrPage(0, dipsw)

        mirroringType = if (dipsw.bit4) HORIZONTAL else VERTICAL
    }
}
