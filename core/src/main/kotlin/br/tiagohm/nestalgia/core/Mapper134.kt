package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE

// https://wiki.nesdev.com/w/index.php/INES_Mapper_134

class Mapper134(console: Console) : MMC3(console) {

    @Volatile private var exReg = 0

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x6001, 0x6001, WRITE)
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        if (softReset) {
            exReg = 0
            updateState()
        }
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, (page and 0xFF) or (exReg and 0x20 shl 3), memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, (page and 0x1F) or (exReg and 0x02 shl 4), memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0x6001) {
            exReg = value
            updateState()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        exReg = s.readInt("exReg")
    }
}
