package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_204

class Mapper204(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val bitMask = addr and 0x06
        val page = bitMask + if (bitMask == 0x06) 0 else addr and 0x01
        selectPrgPage(0, page)
        selectPrgPage(1, bitMask + if (bitMask == 0x06) 1 else addr and 0x01)
        selectChrPage(0, page)
        mirroringType = if (addr.bit4) HORIZONTAL else VERTICAL
    }
}
