package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_320

class Bmc830425C4391T(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    @Volatile private var innerReg = 0
    @Volatile private var outerReg = 0
    @Volatile private var prgMode = false

    override fun initialize() {
        selectChrPage(0, 0)
        updateState()
    }

    private fun updateState() {
        if (prgMode) {
            // UNROM mode
            selectPrgPage(0, (innerReg and 0x07) or (outerReg shl 3))
            selectPrgPage(1, 0x07 or (outerReg shl 3))
        } else {
            // UOROM mode
            selectPrgPage(0, innerReg or (outerReg shl 3))
            selectPrgPage(1, 0x0F or (outerReg shl 3))
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        innerReg = value and 0x0F

        if (addr and 0xFFE0 == 0xF0E0) {
            outerReg = addr and 0x0F
            prgMode = (addr shr 4).bit0
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("innerReg", innerReg)
        s.write("outerReg", outerReg)
        s.write("prgMode", prgMode)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        innerReg = s.readInt("innerReg")
        outerReg = s.readInt("outerReg")
        prgMode = s.readBoolean("prgMode")
    }
}
