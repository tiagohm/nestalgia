package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_170

class Mapper170(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x7000

    override val registerEndAddress = 0x7001

    override val allowRegisterRead = true

    private var reg = 0

    override fun initialize() {
        removeRegisterRange(0x7000, 0x7000, READ)
        removeRegisterRange(0x7001, 0x7001, WRITE)
        addRegisterRange(0x6502, 0x6502, WRITE)
        addRegisterRange(0x7777, 0x7777, READ)

        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        reg = 0
    }

    override fun readRegister(addr: Int): Int {
        return reg or (addr shr 8 and 0x7F)
    }

    override fun writeRegister(addr: Int, value: Int) {
        reg = value shl 1 and 0x80
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
