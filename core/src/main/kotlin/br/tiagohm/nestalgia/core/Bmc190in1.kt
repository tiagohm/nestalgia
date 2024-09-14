package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_300

class Bmc190in1(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value shr 2 and 0x07)
        selectPrgPage(1, value shr 2 and 0x07)
        selectChrPage(0, value shr 2 and 0x07)
        mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
    }
}
