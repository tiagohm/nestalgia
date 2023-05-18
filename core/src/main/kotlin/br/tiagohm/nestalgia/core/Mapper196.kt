package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_196

class Mapper196 : MMC3() {

    private val exReg = UByteArray(2)

    override fun init() {
        super.init()

        exReg.fill(0U)

        addRegisterRange(0x6000U, 0x6FFFU, MemoryOperation.WRITE)
    }

    override fun updatePrgMapping() {
        if (exReg[0].isNonZero) {
            // Used by Master Fighter II (Unl) (UT1374 PCB)
            selectPrgPage4x(0U, (exReg[1].toUInt() shl 2).toUShort())
        } else {
            super.updatePrgMapping()
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        when {
            addr < 0x8000U -> {
                exReg[0] = 1U
                exReg[1] = (value and 0x0FU) or (value shr 4)
                updatePrgMapping()
            }
            addr >= 0xC000U -> {
                super.writeRegister(
                    ((addr.toUInt() and 0xFFFEU) or ((addr.toUInt() shr 2) and 0x01U) or ((addr.toUInt() shr 3) and 0x01U)).toUShort(),
                    value
                )
            }
            else -> {
                super.writeRegister(
                    ((addr.toUInt() and 0xFFFEU) or ((addr.toUInt() shr 2) and 0x01U) or ((addr.toUInt() shr 3) and 0x01U) or ((addr.toUInt() shr 1) and 0x01U)).toUShort(),
                    value
                )
            }
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
}
