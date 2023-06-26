package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_234

class Mapper234(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0xFF80

    override val registerEndAddress = 0xFF9F

    override val allowRegisterRead = true

    override val hasBusConflicts = true

    private val regs = IntArray(2)

    override fun initialize() {
        addRegisterRange(0xFFE8, 0xFFF8, READ_WRITE)
        updateState()
    }

    private fun updateState() {
        if (regs[0].bit6) {
            // NINA-03 mode.
            selectPrgPage(0, regs[0] and 0x0E or (regs[1] and 0x01))
            selectChrPage(0, regs[0] shl 2 and 0x38 or (regs[1] shr 4 and 0x07))
        } else {
            // CNROM mode.
            selectPrgPage(0, regs[0] and 0x0F)
            selectChrPage(0, regs[0] shl 2 and 0x3C or (regs[1] shr 4 and 0x03))
        }

        mirroringType = if (regs[0].bit7) HORIZONTAL else VERTICAL
    }

    override fun readRegister(addr: Int): Int {
        val value = internalRead(addr)

        if (addr <= 0xFF9F) {
            if (regs[0] and 0x3F == 0) {
                regs[0] = value
                updateState()
            }
        } else {
            regs[1] = value and 0x71
            updateState()
        }

        return value
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr <= 0xFF9F) {
            if (regs[0] and 0x3F == 0) {
                regs[0] = value
                updateState()
            }
        } else {
            regs[1] = value and 0x71
            updateState()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
    }
}
