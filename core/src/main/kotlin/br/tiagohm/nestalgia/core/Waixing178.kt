package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_178

class Waixing178(console: Console) : Mapper(console) {

    private var exReg = IntArray(4)

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val workRamSize = 0x8000

    override val registerStartAddress = 0x4800

    override val registerEndAddress = 0x4FFF

    override fun initialize() {
        exReg.fill(0)
        updateState()
        selectChrPage(0, 0)
    }

    private fun updateState() {
        val sbank = exReg[1] and 0x07
        val bbank = exReg[2]

        if (exReg[0].bit1) {
            selectPrgPage(0, bbank shl 3 or sbank)

            if (exReg[0].bit2) {
                selectPrgPage(1, (bbank shl 3) or 0x06 or (sbank and 0x01))
            } else {
                selectPrgPage(1, (bbank shl 3) or 0x07)
            }
        } else {
            val bank = bbank shl 3 or sbank

            if (exReg[0].bit2) {
                selectPrgPage(0, bank)
                selectPrgPage(1, bank)
            } else {
                selectPrgPage2x(0, bank)
            }
        }

        addCpuMemoryMapping(
            0x6000,
            0x7FFF,
            exReg[3] and 0x03,
            PrgMemoryType.WRAM,
            MemoryAccessType.READ_WRITE,
        )

        mirroringType = if (exReg[0].bit0) MirroringType.HORIZONTAL
        else MirroringType.VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        exReg[addr and 0x03] = value
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArrayOrFill("exReg", exReg, 0)

        updateState()
    }
}
