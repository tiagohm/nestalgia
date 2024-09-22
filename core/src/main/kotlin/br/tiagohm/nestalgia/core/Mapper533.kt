package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://www.nesdev.org/wiki/NES_2.0_Mapper_533

class Mapper533(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    @Volatile private var latch = 0

    override fun initialize() {
        removeRegisterRange(0x0000, 0xFFFF, READ_WRITE)

        addRegisterRange(0x8000, 0xFFFF, WRITE)
        addRegisterRange(0xE000, 0xEFFF, READ)

        selectPrgPage(0, 0)
        selectChrPage(0, 0)

        writeRegister(0x8000, 0)
    }

    override fun readRegister(addr: Int): Int {
        return (prgRom[0x6000 or (addr and 0xFFF)] and 0xF0) or latch
    }

    override fun writeRegister(addr: Int, value: Int) {
        latch = value shr 4
        selectChrPage(0, latch and 0x01)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("latch", latch)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        latch = s.readInt("latch")
    }
}
