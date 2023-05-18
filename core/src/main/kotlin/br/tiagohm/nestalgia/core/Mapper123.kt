package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_123

class Mapper123 : MMC3() {

    private val exReg = UByteArray(2)

    override fun init() {
        super.init()

        exReg[0] = 0U
        exReg[1] = 0U

        addRegisterRange(0x5001U, 0x5FFFU, MemoryOperation.WRITE)
    }

    override fun updatePrgMapping() {
        if (exReg[0].bit6) {
            val bank = ((exReg[0] and 0x05U) or
                ((exReg[0] and 0x08U) shr 2) or
                ((exReg[0] and 0x20U) shr 2)).toUInt()

            if (exReg[0].bit1) {
                selectPrgPage4x(0U, ((bank and 0xFEU) shl 1).toUShort())
            } else {
                val page = (bank shl 1).toUShort()
                selectPrgPage2x(0U, page)
                selectPrgPage2x(1U, page)
            }
        } else {
            super.updatePrgMapping()
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr < 0x8000U && (addr.toInt() and 0x0800) == 0x0800) {
            exReg[addr.toInt() and 0x01] = value
            updatePrgMapping()
        } else if (addr < 0xA000U) {
            when (addr.toInt() and 0x8001) {
                0x8000 -> super.writeRegister(0x8000U, (value and 0xC0U) or SECURITY[value.toInt() and 0x07])
                0x8001 -> super.writeRegister(0x8001U, value)
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

        s.readUByteArray("exReg")?.copyInto(exReg) ?: exReg.fill(0U)
    }

    companion object {

        @JvmStatic private val SECURITY = ubyteArrayOf(0U, 3U, 1U, 5U, 6U, 7U, 2U, 4U)
    }
}
