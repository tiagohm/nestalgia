package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperation.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_217

class Mapper217(console: Console) : MMC3(console) {

    private val exRegs = IntArray(4)

    override fun initialize() {
        addRegisterRange(0x5000, 0x5001, WRITE)
        addRegisterRange(0x5007, 0x5007, WRITE)

        super.initialize()
    }

    override fun reset(softReset: Boolean) {
        exRegs[0] = 0
        exRegs[1] = 0xFF
        exRegs[2] = 0x03
        exRegs[3] = 0

        super.reset(softReset)

        updateState()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        val newPage = if (exRegs[1].bit3) {
            page
        } else {
            exRegs[1] shl 3 and 0x80 or (page and 0x7F)
        }

        super.selectChrPage(slot, exRegs[1] shl 8 and 0x0300 or newPage, memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        val newPage = if (exRegs[1].bit3) {
            page and 0x1F
        } else {
            page and 0x0F or (exRegs[1] and 0x10)
        }

        super.selectPrgPage(slot, exRegs[1] shl 5 and 0x60 or newPage, memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            when (addr) {
                0x5000 -> {
                    exRegs[0] = value

                    if (value.bit7) {
                        val newValue = (value and 0x0F) or (exRegs[1] shl 4 and 0x30) shl 1
                        selectPrgPage(0, newValue)
                        selectPrgPage(1, newValue + 1)
                        selectPrgPage(2, newValue)
                        selectPrgPage(3, newValue + 1)
                    } else {
                        updatePrgMapping()
                    }
                }
                0x5001 -> if (exRegs[1] != value) {
                    exRegs[1] = value
                    updatePrgMapping()
                }
                0x5007 -> exRegs[2] = value
            }
        } else {
            when (addr and 0xE001) {
                0x8000 -> super.writeRegister(if (exRegs[2] != 0) 0xC000 else 0x8000, value)
                0x8001 -> if (exRegs[2] != 0) {
                    exRegs[3] = 1
                    super.writeRegister(0x8000, value and 0xC0 or LUT[value and 0x07])
                } else {
                    super.writeRegister(0x8001, value)
                }
                0xA000 -> if (exRegs[2] != 0) {
                    if (exRegs[3] != 0 && (!exRegs[0].bit7 || currentRegister < 6)) {
                        exRegs[3] = 0
                        super.writeRegister(0x8001, value)
                    }
                } else {
                    mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
                }
                0xA001 -> if (exRegs[2] != 0) {
                    mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
                } else {
                    super.writeRegister(0xA001, value)
                }
                else -> super.writeRegister(addr, value)
            }
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

    companion object {

        @JvmStatic private val LUT = intArrayOf(0, 6, 3, 7, 5, 2, 4, 1)
    }
}
