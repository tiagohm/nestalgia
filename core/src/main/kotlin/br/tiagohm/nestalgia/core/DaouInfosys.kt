package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_156

class DaouInfosys(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x400

    override val registerStartAddress = 0xC000

    override val registerEndAddress = 0xC014

    private val chrLow = IntArray(8)
    private val chrHigh = IntArray(8)

    override fun initialize() {
        selectPrgPage(1, -1)
        mirroringType = SCREEN_A_ONLY
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateChrBanks() {
        repeat(8) {
            selectChrPage(it, chrHigh[it] shl 8 or chrLow[it])
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0xC000, 0xC001, 0xC002, 0xC003, 0xC004, 0xC005,
            0xC006, 0xC007, 0xC008, 0xC009, 0xC00A, 0xC00B,
            0xC00C, 0xC00D, 0xC00E, 0xC00F -> {
                val bank = (addr and 0x03) + if (addr >= 0xC008) 4 else 0
                val chr = if (addr.bit2) chrHigh else chrLow
                chr[bank] = value
                updateChrBanks()
            }
            0xC010 -> selectPrgPage(0, value)
            0xC014 -> mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chrLow", chrLow)
        s.write("chrHigh", chrHigh)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("chrLow", chrLow)
        s.readIntArray("chrHigh", chrHigh)

        updateChrBanks()
    }
}
