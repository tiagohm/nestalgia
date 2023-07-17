package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_261

class Bmc810544CA1(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {}

    override fun reset(softReset: Boolean) {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val bank = addr shr 6 and 0xFFFE

        if (addr.bit6) {
            selectPrgPage2x(0, bank)
        } else {
            selectPrgPage(0, bank or (addr shr 5 and 0x01))
            selectPrgPage(1, bank or (addr shr 5 and 0x01))
        }

        selectChrPage(0, addr and 0x0F)
        mirroringType = if (addr.bit4) HORIZONTAL else VERTICAL
    }
}
