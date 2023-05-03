package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_254

class Mapper254 : MMC3() {

    private val exReg = UByteArray(2)

    override val allowRegisterRead = true

    override fun init() {
        super.init()

        addRegisterRange(0x6000U, 0x7FFFU, MemoryOperation.READ)
        removeRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.READ)
    }

    override fun readRegister(addr: UShort): UByte {
        val value = internalRead(addr)
        return if (exReg[0].isNonZero) value else value xor exReg[1]
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        when (addr.toInt()) {
            0x8000 -> exReg[0] = 0xFFU
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

        s.readUByteArray("exReg")?.copyInto(exReg) ?: exReg.fill(0U)
    }
}