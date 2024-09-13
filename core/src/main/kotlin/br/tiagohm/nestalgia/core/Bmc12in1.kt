package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_331

class Bmc12in1(console: Console) : Mapper(console) {

    private val regs = IntArray(2)
    @Volatile private var mode = 0

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x1000

    override fun initialize() {
        updateState()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateState() {
        val bank = mode and 0x03 shl 3

        selectChrPage(0, regs[0] shr 3 or (bank shl 2))
        selectChrPage(1, regs[1] shr 3 or (bank shl 2))

        if (mode.bit3) {
            selectPrgPage2x(0, bank or (regs[0] and 0x06))
        } else {
            selectPrgPage(0, bank or (regs[0] and 0x07))
            selectPrgPage(1, bank or 0x07)
        }

        mirroringType = if (mode.bit2) HORIZONTAL else VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE000) {
            0xA000 -> regs[0] = value
            0xC000 -> regs[1] = value
            0xE000 -> mode = value and 0x0F
            else -> return
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
        s.write("mode", mode)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
        mode = s.readInt("mode")
    }
}
