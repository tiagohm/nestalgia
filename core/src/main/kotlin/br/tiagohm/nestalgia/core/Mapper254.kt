package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperation.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_254

class Mapper254 : MMC3() {

    private val exReg = IntArray(2)

    override val allowRegisterRead = true

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x6000, 0x7FFF, READ)
        removeRegisterRange(0x8000, 0xFFFF, READ)
    }

    override fun readRegister(addr: Int): Int {
        val value = internalRead(addr)
        return if (exReg[0] != 0) value else value xor exReg[1]
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0x8000 -> exReg[0] = 0xFF
            0xA001 -> exReg[1] = value
        }

        super.writeRegister(addr, value)
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
