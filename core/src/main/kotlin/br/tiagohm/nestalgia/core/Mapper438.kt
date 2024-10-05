package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_437

class Mapper438(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        reset()
    }

    override fun reset(softReset: Boolean) {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        mirroringType = if (value.bit0) HORIZONTAL else VERTICAL

        if (addr.bit0) {
            selectPrgPage2x(0, addr shr 1 and 0xFE)
        } else {
            selectPrgPage(0, addr shr 1)
            selectPrgPage(1, addr shr 1)
        }

        selectChrPage(0, value shr 1)
    }
}
