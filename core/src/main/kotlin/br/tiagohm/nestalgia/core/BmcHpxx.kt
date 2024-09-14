package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_260

class BmcHpxx(console: Console) : MMC3(console) {

    private val exRegs = IntArray(5)
    @Volatile private var locked = false

    override val dipSwitchCount = 4

    override val allowRegisterRead = true

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x5000, 0x5FFF, READ_WRITE)
        removeRegisterRange(0x8000, 0xFFFF, READ)
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)
        exRegs.fill(0)
        locked = false
        resetMMC3()
        updateState()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (exRegs[0].bit2) {
            when (exRegs[0] and 0x03) {
                0, 1 -> selectChrPage8x(0, exRegs[2] and 0x3F shl 3)
                2 -> selectChrPage8x(0, exRegs[2] and 0x3E or (exRegs[4] and 0x01) shl 3)
                3 -> selectChrPage8x(0, exRegs[2] and 0x3C or (exRegs[4] and 0x03) shl 3)
            }
        } else {
            val base: Int
            val mask: Int

            if (exRegs[0].bit0) {
                base = exRegs[2] and 0x30
                mask = 0x7F
            } else {
                base = exRegs[2] and 0x20
                mask = 0xFF
            }

            super.selectChrPage(slot, page and mask or (base shl 3), memoryType)
        }
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        if (exRegs[0].bit2) {
            if (exRegs[0] and 0x0F == 0x04) {
                selectPrgPage2x(0, exRegs[1] and 0x1F shl 1)
                selectPrgPage2x(1, exRegs[1] and 0x1F shl 1)
            } else {
                selectPrgPage4x(0, exRegs[1] and 0x1E shl 1)
            }
        } else {
            val base: Int
            val mask: Int

            if (exRegs[0].bit1) {
                base = exRegs[1] and 0x18
                mask = 0x0F
            } else {
                base = exRegs[1] and 0x10
                mask = 0x1F
            }
            super.selectPrgPage(slot, page and mask or (base shl 1), memoryType)
        }
    }

    override fun updateMirroring() {
        if (exRegs[0].bit2) {
            mirroringType = if (exRegs[4].bit2) VERTICAL else HORIZONTAL
        } else {
            super.updateMirroring()
        }
    }

    override fun readRegister(addr: Int): Int {
        return dipSwitches
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (!locked) {
                exRegs[addr and 0x03] = value
                locked = !value.bit7
                updatePrgMapping()
                updateChrMapping()
            }
        } else {
            if (exRegs[0].bit2) {
                exRegs[4] = value
                updateChrMapping()
            } else {
                super.writeRegister(addr, value)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exRegs", exRegs)
        s.write("locked", locked)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("exRegs", exRegs)
        locked = s.readBoolean("locked")
    }
}
