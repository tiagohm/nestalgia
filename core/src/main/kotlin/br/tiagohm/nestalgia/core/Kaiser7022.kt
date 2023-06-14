package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperation.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_175

class Kaiser7022(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override val allowRegisterRead = true

    private var reg = 0

    override fun initialize() {
        removeRegisterRange(0x8000, 0xFFFF, READ)
        addRegisterRange(0xFFFC, 0xFFFC, ANY)
        selectPrgPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        reg = 0
        readRegister(0xFFFC)
    }

    override fun readRegister(addr: Int): Int {
        selectChrPage(0, reg)
        selectPrgPage(0, reg)
        selectPrgPage(1, reg)

        return internalRead(addr)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0x8000 -> mirroringType = if (value.bit2) HORIZONTAL else VERTICAL
            0xA000 -> reg = value and 0x0F
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
}
