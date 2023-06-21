package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperation.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_224

class Mapper224(console: Console) : MMC3(console) {

    private var outerBank = 0

    override fun initialize() {
        addRegisterRange(0x5000, 0x5003, WRITE)

        super.initialize()
    }

    override fun updatePrgMapping() {
        val outerBank = outerBank shl 6

        if (prgMode == 0) {
            selectPrgPage(0, registers[6] and 0x3F or outerBank)
            selectPrgPage(1, registers[7] and 0x3F or outerBank)
            selectPrgPage(2, 0x3E or outerBank)
            selectPrgPage(3, 0x3F or outerBank)
        } else if (prgMode == 1) {
            selectPrgPage(0, 0x3E or outerBank)
            selectPrgPage(1, registers[6] and 0x3F or outerBank)
            selectPrgPage(2, registers[7] and 0x3F or outerBank)
            selectPrgPage(3, 0x3F or outerBank)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (addr == 0x5000) {
                outerBank = value shr 2 and 0x01
                updatePrgMapping()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("outerBank", outerBank)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        outerBank = s.readInt("outerBank")
    }
}
