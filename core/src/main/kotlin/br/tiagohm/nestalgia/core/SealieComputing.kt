package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_029

class SealieComputing : Mapper() {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val workRamSize = 0x2000

    override val chrRamSize = 0x8000

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        selectPrgPage(1, -1)

        // It is hard-wired for vertical mirroring", but no need to enforce this,
        // just need proper iNES headers.
        // mirroringType = VERTICAL

        // Contains 8KB of WRAM mounted in the usual place.
        addCpuMemoryMapping(0x6000, 0x7FFF, 0, WRAM, READ_WRITE)
    }
}
