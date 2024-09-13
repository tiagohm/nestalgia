package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_235

class Bmc235(console: Console) : Mapper(console) {

    @Volatile private var openBus = false

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage2x(0, 0)
        selectChrPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        selectPrgPage2x(0, 0)
        openBus = false
    }

    override fun writeRegister(addr: Int, value: Int) {
        mirroringType = when {
            addr and 0x0400 != 0 -> SCREEN_A_ONLY
            addr and 0x2000 != 0 -> HORIZONTAL
            else -> VERTICAL
        }

        val mode = when (prgPageCount) {
            64 -> 0
            128 -> 1
            256 -> 2
            else -> 3
        }

        val i = addr shr 8 and 0x03
        val bank = CONFIG[mode][i][0] or (addr.loByte and 0x1F)
        openBus = false

        when {
            CONFIG[mode][i][1] == 1 -> {
                openBus = true
                removeCpuMemoryMapping(0x8000, 0xFFFF)
            }
            addr and 0x800 != 0 -> {
                val b = (bank shl 1) or (addr shr 12 and 0x01)
                selectPrgPage(0, b)
                selectPrgPage(1, b)
            }
            else -> {
                selectPrgPage2x(0, bank shl 1)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("openBus", openBus)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        openBus = s.readBoolean("openBus")

        if (openBus) {
            removeCpuMemoryMapping(0x8000, 0xFFFF)
        }
    }

    companion object {

        private val CONFIG = arrayOf(
            arrayOf(intArrayOf(0x00, 0), intArrayOf(0x00, 1), intArrayOf(0x00, 1), intArrayOf(0x00, 1)),
            arrayOf(intArrayOf(0x00, 0), intArrayOf(0x00, 1), intArrayOf(0x20, 0), intArrayOf(0x00, 1)),
            arrayOf(intArrayOf(0x00, 0), intArrayOf(0x00, 1), intArrayOf(0x20, 0), intArrayOf(0x40, 0)),
            arrayOf(intArrayOf(0x00, 0), intArrayOf(0x20, 0), intArrayOf(0x40, 0), intArrayOf(0x60, 0)),
        )
    }
}
