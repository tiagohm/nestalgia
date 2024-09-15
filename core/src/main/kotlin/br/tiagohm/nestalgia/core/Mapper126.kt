package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.DEFAULT
import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE

// https://wiki.nesdev.com/w/index.php/INES_Mapper_126

class Mapper126(console: Console) : MMC3(console) {

    private val exRegs = IntArray(4)

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x6000, 0x8000, WRITE)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        val reg = exRegs[0]

        var newPage = page and (reg.inv() shr 2 and 0x10 or 0x0F)
        newPage = newPage or (reg and (0x06 or (reg and 0x40 shr 6)) shl 4 or (reg and 0x10 shl 3))

        if (exRegs[3] and 0x03 == 0) {
            super.selectPrgPage(slot, newPage, memoryType)
        } else if (prgMode.toInt() shl 1 == slot) {
            if (exRegs[3] and 0x03 != 0) {
                super.selectPrgPage(0, newPage, memoryType)
                super.selectPrgPage(1, newPage + 1, memoryType)
                super.selectPrgPage(2, newPage + 2, memoryType)
                super.selectPrgPage(3, newPage + 3, memoryType)
            } else {
                super.selectPrgPage(0, newPage, memoryType)
                super.selectPrgPage(1, newPage + 1, memoryType)
                super.selectPrgPage(2, newPage, memoryType)
                super.selectPrgPage(3, newPage + 1, memoryType)
            }
        }
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (!exRegs[3].bit4) {
            super.selectChrPage(slot, chrOuterBank or (page and (exRegs[0] and 0x80) - 1), memoryType)
        }
    }

    private val chrOuterBank: Int
        get() {
            val reg = exRegs[0]

            return (reg.inv() and 0x0080 and exRegs[2]) or
                (reg shl 4 and 0x0080 and reg) or
                (reg shl 3 and 0x0100) or
                (reg shl 5 and 0x0200)
        }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            val newAddr = addr and 0x03

            if (newAddr == 0x01 || newAddr == 0x02 || !exRegs[3].bit7) {
                if (exRegs[newAddr] != value) {
                    exRegs[newAddr] = value

                    if (exRegs[3].bit4) {
                        val page = chrOuterBank or (exRegs[2] and 0x0F shl 3)

                        repeat(8) {
                            super.selectChrPage(it, page + it, DEFAULT)
                        }
                    } else {
                        updateChrMapping()
                    }

                    updatePrgMapping()
                }
            }
        } else {
            super.writeRegister(addr, value)
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
