package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_335

class BmcCtc09(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0)
        writeRegister(0xC000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr >= 0xC000) {
            if (value.bit4) {
                selectPrgPage(0, (value shl 1 and 0x0E) or (value shr 3 and 0x01))
                selectPrgPage(1, (value shl 1 and 0x0E) or (value shr 3 and 0x01))
            } else {
                selectPrgPage2x(0, value and 0x07 shl 1)
            }

            mirroringType = if (value.bit6) HORIZONTAL else VERTICAL
        } else {
            selectChrPage(0, value and 0x0F)
        }
    }
}
