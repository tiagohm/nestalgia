package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE

class Mapper153(console: Console) : BandaiFgc(console) {

    override fun initialize() {
        super.initialize()

        // For iNES Mapper 153 (with SRAM), the writeable ports must
        // only be mirrored across $8000-$FFFF.

        // Mapper 153 has regular save ram from $6000-$7FFF, need to remove the
        // register for both read & writes.
        removeRegisterRange(0x6000, 0x7FFF, READ_WRITE)
    }
}
