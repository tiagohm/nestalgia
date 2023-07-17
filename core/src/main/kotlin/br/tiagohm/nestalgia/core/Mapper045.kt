package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_045

class Mapper045(console: Console) : MMC3(console) {

    private var regIndex = 0
    private val reg = IntArray(4)

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        super.initialize()

        // Needed by Famicom Yarou Vol 1 - Game apparently writes to CHR RAM before initializing the registers
        registers[0] = 0
        registers[1] = 2
        registers[2] = 4
        registers[3] = 5
        registers[4] = 6
        registers[5] = 7

        updateChrMapping()
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        addRegisterRange(0x6000, 0x7FFF, READ_WRITE)

        regIndex = 0
        resetReg()

        updateState()
    }

    private fun resetReg() {
        reg[0] = 0
        reg[1] = 0
        reg[2] = 0x0F
        reg[3] = 0
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(
            slot,
            if (!hasChrRam) {
                val p = page and (0xFF shr (0x0F - (reg[2] and 0x0F)))
                p or (reg[0] or ((reg[2] and 0xF0) shl 4))
            } else {
                page
            },
            memoryType
        )
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, page and (0x3F xor (reg[3] and 0x3F)) or reg[1], memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (!reg[3].bit6) {
                reg[regIndex] = value
                regIndex = (regIndex + 1) and 0x03
            } else {
                removeRegisterRange(0x6000, 0x7FFF, READ_WRITE)
            }

            updateState()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regIndex", regIndex)
        s.write("reg", reg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        regIndex = s.readInt("regIndex")
        s.readIntArray("reg", reg) ?: resetReg()

        if (reg[3].bit6) {
            removeRegisterRange(0x6000, 0x7FFF, READ_WRITE)
        }
    }
}
