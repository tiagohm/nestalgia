package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE

// https://www.nesdev.org/wiki/NES_2.0_Mapper_258

class Unl158B(console: Console) : MMC3(console) {

    override val allowRegisterRead = true

    @Volatile private var reg = 0

    override fun initialize() {
        addRegisterRange(0x5000, 0x5FFF, READ_WRITE)
        removeRegisterRange(0x8000, 0xFFFF, READ)

        super.initialize()
    }

    override fun reset(softReset: Boolean) {
        reg = 0
        resetMMC3()
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        if (reg.bit7) {
            val bank = reg and 0x07

            if (reg.bit5) {
                selectPrgPage4x(0, bank and 0x06 shl 1)
            } else {
                selectPrgPage2x(0, bank shl 1)
                selectPrgPage2x(1, bank shl 1)
            }
        } else {
            super.selectPrgPage(slot, page and 0x0F, memoryType)
        }
    }

    override fun readRegister(addr: Int): Int {
        return console.memoryManager.openBus() or PROTECTION_LUT[addr and 0x07]
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr <= 0x5FFF) {
            if (addr and 0x07 == 0) {
                reg = value
                updatePrgMapping()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("reg", reg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        reg = s.readInt("reg")
    }

    companion object {

        private val PROTECTION_LUT = intArrayOf(0x00, 0x00, 0x00, 0x01, 0x02, 0x04, 0x0F, 0x00)
    }
}
