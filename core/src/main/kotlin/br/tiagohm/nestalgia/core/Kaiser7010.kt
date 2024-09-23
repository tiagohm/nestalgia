package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.PrgMemoryType.ROM

// https://www.nesdev.org/wiki/NES_2.0_Mapper_554

class Kaiser7010(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    override fun initialize() {
        selectPrgPage(0, 10)
        selectPrgPage(1, 11)
        selectPrgPage(2, 6)
        selectPrgPage(3, 7)
        selectChrPage(0, 0)
        addCpuMemoryMapping(0x6000, 0x7FFF, 0, ROM)
    }

    override fun readRegister(addr: Int): Int {
        if (addr in 0xCAB6..0xCAD7) {
            selectChrPage(0, addr shr 2 and 0x0F)
            addCpuMemoryMapping(0x6000, 0x7FFF, addr shr 2 and 0x0F, ROM)
        } else if ((addr and 0xFFFE == 0xEBE2) || (addr and 0xFFFE == 0xEE32)) {
            selectChrPage(0, addr shr 2 and 0x0F)
            addCpuMemoryMapping(0x6000, 0x7FFF, addr shr 2 and 0x0F, ROM)
        } else if (addr and 0xFFFE == 0xFFFC) {
            selectChrPage(0, addr shr 2 and 0x0F)
            addCpuMemoryMapping(0x6000, 0x7FFF, addr shr 2 and 0x0F, ROM)
        }

        return internalRead(addr)
    }
}
