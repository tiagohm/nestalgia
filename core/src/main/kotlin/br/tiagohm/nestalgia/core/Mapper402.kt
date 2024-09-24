package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE
import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL
import br.tiagohm.nestalgia.core.PrgMemoryType.ROM

// https://www.nesdev.org/wiki/NES_2.0_Mapper_402

class Mapper402(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override fun initialize() {
        reset()
    }

    override fun reset(softReset: Boolean) {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x0800 != 0) {
            addCpuMemoryMapping(0x6000, 0x7FFF, (((addr and 0x1F) shl 1) or 0x03), ROM)
        } else {
            removeCpuMemoryMapping(0x6000, 0x7FFF)
        }

        if (addr.bit6) {
            selectPrgPage2x(0, (addr and 0x1F) shl 1)
            selectPrgPage2x(1, (addr and 0x1F) shl 1)
        } else {
            selectPrgPage4x(0, (addr and 0x1E) shl 1)
        }

        selectChrPage(0, 0)
        addPpuMemoryMapping(0, 0x1FFF, 0, ChrMemoryType.DEFAULT, if (addr and 0x0400 == 0) READ else READ_WRITE)

        mirroringType = if (addr.bit7) HORIZONTAL else VERTICAL
    }
}
