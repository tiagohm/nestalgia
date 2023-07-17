package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_196

class Mapper196(console: Console) : MMC3(console) {

    private val exReg = IntArray(2)

    override fun initialize() {
        super.initialize()

        exReg.fill(0)

        addRegisterRange(0x6000, 0x6FFF, WRITE)
    }

    override fun updatePrgMapping() {
        if (exReg[0] != 0) {
            // Used by Master Fighter II (Unl) (UT1374 PCB)
            selectPrgPage4x(0, exReg[1] shl 2)
        } else {
            super.updatePrgMapping()
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when {
            addr < 0x8000 -> {
                exReg[0] = 1
                exReg[1] = (value and 0x0F) or (value shr 4)
                updatePrgMapping()
            }
            addr >= 0xC000 -> {
                super.writeRegister((addr and 0xFFFE) or (addr shr 2 and 0x01) or (addr shr 3 and 0x01), value)
            }
            else -> {
                super.writeRegister((addr and 0xFFFE) or (addr shr 2 and 0x01) or (addr shr 3 and 0x01) or (addr shr 1 and 0x01), value)
            }
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
}
