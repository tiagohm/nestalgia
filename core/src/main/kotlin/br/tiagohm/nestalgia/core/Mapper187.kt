package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_187

class Mapper187(console: Console) : MMC3(console) {

    override val allowRegisterRead = true

    private val exRegs = IntArray(2)

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x5000, 0x5FFF, READ_WRITE)
        addRegisterRange(0x6000, 0x6FFF, WRITE)
        removeRegisterRange(0x8000, 0xFFFF, READ)
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (chrMode != 0 && slot >= 4 || chrMode == 0 && slot < 4) {
            super.selectChrPage(slot, page or 0x100, memoryType)
        } else {
            super.selectChrPage(slot, page, memoryType)
        }
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        if (!exRegs[0].bit7) {
            super.selectPrgPage(slot, page and 0x3F, memoryType)
        } else {
            var exPage = exRegs[0] and 0x1F

            if (exRegs[0].bit5) {
                if (exRegs[0].bit6) {
                    exPage = exPage and 0xFC
                    super.selectPrgPage(0, exPage, memoryType)
                    super.selectPrgPage(1, exPage + 1, memoryType)
                    super.selectPrgPage(2, exPage + 2, memoryType)
                    super.selectPrgPage(3, exPage + 3, memoryType)
                } else {
                    exPage = exPage and 0xFE shl 1
                    super.selectPrgPage(0, exPage, memoryType)
                    super.selectPrgPage(1, exPage + 1, memoryType)
                    super.selectPrgPage(2, exPage + 2, memoryType)
                    super.selectPrgPage(3, exPage + 3, memoryType)
                }
            } else {
                exPage = exPage shl 1
                super.selectPrgPage(0, exPage, memoryType)
                super.selectPrgPage(1, exPage + 1, memoryType)
                super.selectPrgPage(2, exPage, memoryType)
                super.selectPrgPage(3, exPage + 1, memoryType)
            }
        }
    }

    override fun readRegister(addr: Int): Int {
        return SECURITY[exRegs[1] and 0x03]
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (addr == 0x5000 || addr == 0x6000) {
                exRegs[0] = value
                updatePrgMapping()
            }
        } else if (addr == 0x8000) {
            exRegs[1] = 1
            super.writeRegister(addr, value);
        } else if (addr == 0x8001) {
            if (exRegs[1] == 1) {
                super.writeRegister(addr, value);
            }
        } else {
            super.writeRegister(addr, value);
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

        @JvmStatic private val SECURITY = intArrayOf(0x83, 0x83, 0x42, 0x00)
    }
}
