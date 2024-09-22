package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ

// Mapper 126 CHR A18/A19 reversed
// Mapper 422
// Mapper 534 $C000 value inverted

// https://www.nesdev.org/wiki/NES_2.0_Mapper_422

open class Mapper422(console: Console) : MMC3(console) {

    override val dipSwitchCount = 2

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override val allowRegisterRead = true

    protected val exRegs = IntArray(4)
    @Volatile private var solderPad = 0

    open val chrOuterBank
        get() = exRegs[0] shl 4 and 0x380

    override fun initialize() {
        super.initialize()

        removeRegisterRange(0x6000, 0x7FFF, READ)
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        solderPad = dipSwitches
        exRegs.fill(0)
        resetMMC3()
        updateState()
    }

    protected open fun writeMMC3(addr: Int, value: Int) {
        super.writeRegister(addr, value)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        val mask = if (exRegs[0].bit6) 0x0F else 0x1F
        val outerBank = ((exRegs[0] shl 3 and 0x180) and mask.inv()) or (exRegs[0] shl 4 and 0x70)

        if (exRegs[3] and 0x03 == 0) {
            // MMC3
            super.selectPrgPage(slot, outerBank or (page and mask), memoryType)
        } else {
            var newPage = (outerBank and mask.inv()) or (registers[6] and mask)

            if (exRegs[3] and 0x03 == 0x03) {
                // NROM-256
                newPage = newPage and 0xFE
                super.selectPrgPage(0, newPage, memoryType)
                super.selectPrgPage(1, newPage + 1, memoryType)
                super.selectPrgPage(2, newPage + 2, memoryType)
                super.selectPrgPage(3, newPage + 3, memoryType)
            } else {
                // NROM-128
                super.selectPrgPage(0, newPage, memoryType)
                super.selectPrgPage(1, newPage + 1, memoryType)
                super.selectPrgPage(2, newPage, memoryType)
                super.selectPrgPage(3, newPage + 1, memoryType)
            }
        }
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        val mask = if (exRegs[0].bit7) 0x7F else 0xFF

        if (!exRegs[3].bit4) {
            super.selectChrPage(slot, (chrOuterBank and mask.inv()) or (page and mask), memoryType)
        }
    }

    protected fun updateChrCnrom() {
        val mask = if (exRegs[0].bit7) 0x7F else 0xFF
        val page = (chrOuterBank and mask.inv() shr 3) or (exRegs[2] and (mask shr 3))

        repeat(8) {
            super.selectChrPage(it, (page shl 3) + it, ChrMemoryType.DEFAULT)
        }
    }

    override fun readRegister(addr: Int): Int {
        var value = internalRead(addr)

        if (exRegs[1].bit0) {
            value = value and 0x03.inv()
            value = value or (solderPad and 0x03)
        }

        return value
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            writePrgRam(addr, value)

            if (!exRegs[3].bit7) {
                exRegs[addr and 0x03] = value

                if (exRegs[3].bit4) {
                    updateChrCnrom()
                } else {
                    super.updateChrMapping()
                }
                super.updatePrgMapping()
            } else {
                if (addr and 0x03 == 0x02) {
                    val mask = ((exRegs[2].inv() shr 3) and 0x02) or 0x01
                    exRegs[2] = exRegs[2] and mask.inv()
                    exRegs[2] = exRegs[2] or (value and mask)
                    updateChrCnrom()
                }
            }
        } else {
            writeMMC3(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exRegs", exRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("exRegs", exRegs)
    }
}
