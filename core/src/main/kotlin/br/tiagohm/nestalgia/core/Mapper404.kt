package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE

// https://www.nesdev.org/wiki/NES_2.0_Mapper_404

class Mapper404(console: Console) : MMC1(console) {

    @Volatile private var outerPrgBank = 0
    @Volatile private var outerChrBank = 0
    @Volatile private var prgMask = 0
    @Volatile private var reg = 0

    override fun initialize() {
        addRegisterRange(0x6000, 0x7FFF, WRITE)
        super.initialize()
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        outerPrgBank = 0
        outerChrBank = 0
        prgMask = 0x0F
        reg = 0

        updateState()
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, outerChrBank or (page and 0x1F), memoryType)
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, outerPrgBank or (page and prgMask), memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (!reg.bit7) {
                reg = value
                prgMask = if (reg.bit6) 0x07 else 0x0F
                outerPrgBank = (reg and 0x0F shl 3) and prgMask.inv()
                outerChrBank = (reg and 0x0F shl 5)
                updateState()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("outerPrgBank", outerPrgBank)
        s.write("outerChrBank", outerChrBank)
        s.write("prgMask", prgMask)
        s.write("reg", reg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        outerPrgBank = s.readInt("outerPrgBank")
        outerChrBank = s.readInt("outerChrBank")
        prgMask = s.readInt("prgMask")
        reg = s.readInt("reg")
    }
}
