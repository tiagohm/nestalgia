package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE

// https://wiki.nesdev.com/w/index.php/INES_Mapper_216

class Mapper216(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    override fun initialize() {
        writeRegister(0x8000, 0)
        addRegisterRange(0x5000, 0x5000, READ_WRITE)
        removeRegisterRange(0x8000, 0xFFFF, READ)
    }

    override fun readRegister(addr: Int): Int {
        // For Videopoker Bonza?
        return 0
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, addr and 0x01)
        selectChrPage(0, addr and 0x0E shr 1)
    }
}
