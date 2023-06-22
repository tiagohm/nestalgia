package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_189

class Mapper189(console: Console) : MMC3(console) {

    override val registerStartAddress = 0x4120

    private var prgReg = 0

    override fun updateState() {
        super.updateState()

        // $4120-7FFF:  [AAAA BBBB]
        // 'A' and 'B' bits of the $4120 reg seem to be effectively OR'd.
        val prgPage = (prgReg or (prgReg shr 4) and 0x07) * 4
        selectPrgPage(0, prgPage)
        selectPrgPage(1, prgPage + 1)
        selectPrgPage(2, prgPage + 2)
        selectPrgPage(3, prgPage + 3)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr <= 0x7FFF) {
            prgReg = value
            updateState()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg")
    }
}
