package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_299

class Bmc11160(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() = Unit

    override fun reset(softReset: Boolean) {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val bank = value shr 4 and 0x07
        selectPrgPage(0, bank)
        selectChrPage(0, bank shl 2 or (value and 0x03))
        mirroringType = if (value.bit7) VERTICAL else HORIZONTAL
    }
}
