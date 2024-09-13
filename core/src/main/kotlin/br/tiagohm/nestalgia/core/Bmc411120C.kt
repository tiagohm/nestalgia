package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_287

class Bmc411120C(console: Console) : MMC3(console) {

    override val dipSwitchCount = 1

    @Volatile private var exReg = 0

    override fun initialize() {
        addRegisterRange(0x6000, 0xFFFF, WRITE)

        super.initialize()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, page or (exReg and 0x03 shl 7), memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        if (exReg and (0x08 or (dipSwitches shl 2)) != 0) {
            super.selectPrgPage4x(0, exReg shr 4 and 0x03 or 0x0C shl 2, memoryType)
        } else {
            super.selectPrgPage(slot, page and 0x0F or (exReg and 0x03 shl 4), memoryType)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            exReg = addr and 0xFF
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
