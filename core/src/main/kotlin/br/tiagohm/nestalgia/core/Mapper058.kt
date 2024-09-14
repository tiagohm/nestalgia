package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_058

class Mapper058(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 1)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val prgBank = addr and 0x07

        if (addr.bit6) {
            selectPrgPage(0, prgBank)
            selectPrgPage(1, prgBank)
        } else {
            selectPrgPage2x(0, prgBank and 0x06)
        }

        selectChrPage(0, addr shr 3 and 0x07)

        mirroringType = if (addr.bit7) HORIZONTAL else VERTICAL
    }
}
