package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

class Cc21(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x1000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
        selectChrPage(1, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        var latch = addr and 0xFF

        if (addr == 0x8000) {
            latch = value
        }

        if (mChrRomSize == 0x2000) {
            selectChrPage(0, latch and 0x01)
            selectChrPage(1, latch and 0x01)
        } else {
            // Overdumped roms.
            selectChrPage2x(0, latch and 0x01 shl 1)
        }

        mirroringType = if (latch.bit0) SCREEN_B_ONLY else SCREEN_A_ONLY
    }
}
