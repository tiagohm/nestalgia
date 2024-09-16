package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_312

class Kaiser7013B(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, -1)
        selectChrPage(0, 0)
        mirroringType = VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            selectPrgPage(0, value)
        } else {
            mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
        }
    }
}
