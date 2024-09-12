package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_121

class Mapper121(console: Console) : MMC3(console) {

    private val exReg = IntArray(8)

    override val allowRegisterRead = true

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x5000, 0x5FFF, READ_WRITE)
        removeRegisterRange(0x8000, 0xFFFF, READ)
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        resetExRegs()
    }

    private fun resetExRegs() {
        exReg.fill(0)
        exReg[3] = 0x80
    }

    override fun readRegister(addr: Int) = exReg[4]

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            // $5000-$5FFF
            exReg[4] = LOOKUP[value and 0x03]

            if ((addr and 0x5180) == 0x5180) {
                // Hack for Super 3-in-1
                exReg[3] = value
                updateState()
            }
        } else if (addr < 0xA000) {
            // $8000-$9FFF
            when {
                (addr and 0x03) == 0x03 -> {
                    exReg[5] = value
                    updateExRegs()
                    super.writeRegister(0x8000, value)
                }
                addr.loByte.bit0 -> {
                    exReg[6] = (value and 0x01 shl 5) or
                        (value and 0x02 shl 3) or
                        (value and 0x04 shl 1) or
                        (value and 0x08 shr 1) or
                        (value and 0x10 shr 3) or
                        (value and 0x20 shr 5)

                    if (exReg[7] == 0) {
                        updateExRegs()
                    }

                    super.writeRegister(0x8001, value)
                }
                else -> {
                    super.writeRegister(0x8000, value)
                }
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        val o = exReg[3] and 0x80 shr 2

        super.selectPrgPage(slot, (page and 0x1F) or o, memoryType)

        if ((exReg[5] and 0x3F) != 0) {
            super.selectPrgPage(1, exReg[2] or o, memoryType)
            super.selectPrgPage(2, exReg[1] or o, memoryType)
            super.selectPrgPage(3, exReg[0] or o, memoryType)
        }
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (mPrgSize == mChrRomSize) {
            // Hack for Super 3-in-1
            super.selectChrPage(slot, page or (exReg[3] and 0x80 shl 1), memoryType)
        } else if ((slot < 4 && chrMode == 0) || (slot >= 4 && chrMode == 1)) {
            super.selectChrPage(slot, page or 0x100, memoryType)
        } else {
            super.selectChrPage(slot, page, memoryType)
        }
    }

    private fun updateExRegs() {
        when (exReg[5] and 0x3F) {
            0x20,
            0x29,
            0x2B,
            0x3C,
            0x3F -> {
                exReg[7] = 1
                exReg[0] = exReg[6]
            }
            0x26 -> {
                exReg[7] = 0
                exReg[0] = exReg[6]
            }
            0x2C -> {
                exReg[7] = 1

                if (exReg[6] != 0) {
                    exReg[0] = exReg[6]
                }
            }
            0x28 -> {
                exReg[7] = 0
                exReg[1] = exReg[6]
            }
            0x2A -> {
                exReg[7] = 0
                exReg[2] = exReg[6]
            }
            0x2F -> Unit
            else -> exReg[5] = 0
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("exReg", exReg) ?: resetExRegs()
    }

    companion object {

        private val LOOKUP = intArrayOf(0x83, 0x83, 0x42, 0x00)
    }
}
