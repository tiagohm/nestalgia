package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_178

@ExperimentalUnsignedTypes
class Waixing178 : Mapper() {

    private var exReg = UByteArray(4)

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x2000U

    override val workRamSize = 0x8000U

    override val registerStartAddress: UShort = 0x4800U

    override val registerEndAddress: UShort = 0x4FFFU

    override fun init() {
        exReg.fill(0U)
        updateState()
        selectChrPage(0U, 0U)
    }

    private fun updateState() {
        val sbank = (exReg[1] and 0x07U).toUInt()
        val bbank = exReg[2].toUInt()

        if (exReg[0].bit1) {
            selectPrgPage(0U, ((bbank shl 3) or sbank).toUShort())

            if (exReg[0].bit2) {
                selectPrgPage(1U, ((bbank shl 3) or 0x06U or (sbank and 0x01U)).toUShort())
            } else {
                selectPrgPage(1U, ((bbank shl 3) or 0x07U).toUShort())
            }
        } else {
            val bank = ((bbank shl 3) or sbank).toUShort()

            if (exReg[0].bit2) {
                selectPrgPage(0U, bank)
                selectPrgPage(1U, bank)
            } else {
                selectPrgPage2x(0U, bank)
            }
        }

        setCpuMemoryMapping(
            0x6000U,
            0x7FFFU,
            (exReg[3].toInt() and 0x03).toShort(),
            PrgMemoryType.WRAM,
            MemoryAccessType.READ_WRITE
        )

        mirroringType = if (exReg[0].bit0) MirroringType.HORIZONTAL else MirroringType.VERTICAL
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        exReg[addr.toInt() and 0x03] = value
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readUByteArray("exReg")?.copyInto(exReg) ?: exReg.fill(0U)

        updateState()
    }
}