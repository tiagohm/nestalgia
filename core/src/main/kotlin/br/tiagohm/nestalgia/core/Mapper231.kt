package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_231

class Mapper231(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        reset()
        selectChrPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        selectPrgPage(0, 0)
        selectPrgPage(1, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val prgBank = addr shr 5 and 0x01 or (addr and 0x1E)
        selectPrgPage(0, prgBank and 0x1E)
        selectPrgPage(1, prgBank)
        mirroringType = if (addr.bit7) HORIZONTAL else VERTICAL
    }
}
