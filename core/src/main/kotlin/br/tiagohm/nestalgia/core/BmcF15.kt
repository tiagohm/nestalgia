package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_259

class BmcF15(console: Console) : MMC3(console) {

    @Volatile private var exReg = 0

    override fun initialize() {
        addRegisterRange(0x6000, 0xFFFF, WRITE)

        super.initialize()
    }

    override fun updatePrgMapping() {
        val bank = exReg and 0x0F
        val mode = exReg and 0x08 shr 3
        val mask = mode.inv()
        selectPrgPage2x(0, bank and mask shl 1)
        selectPrgPage2x(1, bank and mask or mode shl 1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (state.regA001.bit7) {
                exReg = value and 0x0F
                updatePrgMapping()
            }
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
