package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE

// https://wiki.nesdev.com/w/index.php/INES_Mapper_123

class Mapper123(console: Console) : MMC3(console) {

    private val exReg = IntArray(2)

    override fun initialize() {
        super.initialize()

        exReg[0] = 0
        exReg[1] = 0

        addRegisterRange(0x5001, 0x5FFF, WRITE)
    }

    override fun updatePrgMapping() {
        if (exReg[0].bit6) {
            val bank = (exReg[0] and 0x05) or
                (exReg[0] and 0x08 shr 2) or
                (exReg[0] and 0x20 shr 2)

            if (exReg[0].bit1) {
                selectPrgPage4x(0, bank and 0xFE shl 1)
            } else {
                val page = bank shl 1
                selectPrgPage2x(0, page)
                selectPrgPage2x(1, page)
            }
        } else {
            super.updatePrgMapping()
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000 && (addr and 0x0800) == 0x0800) {
            exReg[addr and 0x01] = value
            updatePrgMapping()
        } else if (addr < 0xA000) {
            when (addr and 0x8001) {
                0x8000 -> super.writeRegister(0x8000, (value and 0xC0) or SECURITY[value and 0x07])
                0x8001 -> super.writeRegister(0x8001, value)
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

        s.readIntArrayOrFill("exReg", exReg, 0)
    }

    companion object {

        private val SECURITY = intArrayOf(0, 3, 1, 5, 6, 7, 2, 4)
    }
}
