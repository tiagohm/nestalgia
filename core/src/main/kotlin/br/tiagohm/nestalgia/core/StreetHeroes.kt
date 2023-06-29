package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_262

class StreetHeroes(console: Console) : MMC3(console) {

    override val chrRamPageSize = 0x2000

    override val chrRamSize = 0x2000

    override val allowRegisterRead = true

    private var exReg = 0
    private var resetSwitch = 0

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x4100, 0x4100, READ_WRITE)
        removeRegisterRange(0x8000, 0xFFFF, READ)
    }

    override fun reset(softReset: Boolean) {
        if (softReset) {
            resetSwitch = resetSwitch xor 0xFF
        }

        updateState()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (exReg.bit6) {
            super.selectChrPage(0, 0, RAM)
        } else {
            when (slot) {
                0, 1 -> super.selectChrPage(slot, page or (exReg and 0x08 shl 5), memoryType)
                2, 3 -> super.selectChrPage(slot, page or (exReg and 0x04 shl 6), memoryType)
                4, 5 -> super.selectChrPage(slot, page or (exReg and 0x01 shl 8), memoryType)
                else -> super.selectChrPage(slot, page or (exReg and 0x02 shl 7), memoryType)
            }
        }
    }

    override fun readRegister(addr: Int): Int {
        return resetSwitch
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0x4100) {
            exReg = value
            updateState()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
        s.write("resetSwitch", resetSwitch)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        exReg = s.readInt("exReg")
        resetSwitch = s.readInt("resetSwitch")
    }
}
