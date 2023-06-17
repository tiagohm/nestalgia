package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_075

class VRC1(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x1000

    private val chrBanks = IntArray(2)

    override fun initialize() {
        selectPrgPage(3, -1)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateChrBanks() {
        selectChrPage(0, chrBanks[0])
        selectChrPage(1, chrBanks[1])
    }

    override fun writeRegister(addr: Int, value: Int) {
        // TODO: Create a setting to enable/disable oversized PRG.
        val allowOversizedPrg = true

        val prgMask = if (allowOversizedPrg) 0xFF else 0x0F

        when (addr and 0xF000) {
            0x8000 -> selectPrgPage(0, value and prgMask)
            0x9000 -> {
                if (mirroringType != FOUR_SCREENS) {
                    // The mirroring bit is ignored if the cartridge is wired for 4-screen VRAM,
                    // as is typical for Vs. System games using the VRC1.
                    mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
                }

                chrBanks[0] = chrBanks[0] and 0x0F or (value and 0x02 shl 3)
                chrBanks[1] = chrBanks[1] and 0x0F or (value and 0x04 shl 2)
                updateChrBanks()
            }
            0xA000 -> selectPrgPage(1, value and prgMask)
            0xC000 -> selectPrgPage(2, value and prgMask)
            0xE000 -> {
                chrBanks[0] = chrBanks[0] and 0x10 or (value and 0x0F)
                updateChrBanks()
            }
            0xF000 -> {
                chrBanks[1] = chrBanks[1] and 0x10 or (value and 0x0F)
                updateChrBanks()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chrBanks", chrBanks)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("chrBanks", chrBanks)
    }
}
