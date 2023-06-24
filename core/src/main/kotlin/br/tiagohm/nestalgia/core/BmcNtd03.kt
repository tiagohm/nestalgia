package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_290

class BmcNtd03(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {}

    override fun reset(softReset: Boolean) {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val prg = addr shr 10 and 0x1E
        val chr = (addr and 0x0300 shr 5) or (addr and 0x07)

        if (addr.bit7) {
            selectPrgPage(0, prg or (addr shr 6 and 1))
            selectPrgPage(1, prg or (addr shr 6 and 1))
        } else {
            selectPrgPage2x(0, prg and 0xFE)
        }

        selectChrPage(0, chr)

        mirroringType = if (addr and 0x400 != 0) HORIZONTAL else VERTICAL
    }
}
