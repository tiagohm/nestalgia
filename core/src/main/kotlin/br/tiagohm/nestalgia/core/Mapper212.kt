package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_212

class Mapper212(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    override fun initialize() {
        addRegisterRange(0x6000, 0x7FFF, READ)
        writeRegister(0x8000, 0)
    }

    override fun readRegister(addr: Int): Int {
        var value = internalRead(addr)

        if (addr and 0xE010 == 0x6000) {
            value = value or 0x80
        }

        return value
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x4000 != 0) {
            selectPrgPage2x(0, addr and 0x06)
        } else {
            selectPrgPage(0, addr and 0x07)
            selectPrgPage(1, addr and 0x07)
        }

        selectChrPage(0, addr and 0x07)

        mirroringType = if (addr.bit3) HORIZONTAL else VERTICAL
    }
}
