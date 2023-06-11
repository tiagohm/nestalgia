package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperation.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_238

class Mapper238 : MMC3() {

    private var exReg = 0

    override val allowRegisterRead = true

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x4020, 0x7FFF, ANY)
        removeRegisterRange(0x8000, 0xFFFF, READ)
    }

    override fun readRegister(addr: Int): Int {
        return exReg
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            exReg = SECURITY_LUT[value and 0x03]
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

    companion object {

        @JvmStatic private val SECURITY_LUT = intArrayOf(0x00, 0x02, 0x02, 0x03)
    }
}
