package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_108

class Bb(console: Console) : Mapper(console) {

    @Volatile private var prgReg = 0
    @Volatile private var chrReg = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override fun initialize() {
        prgReg = 0xFF
        chrReg = 0

        selectPrgPage4x(0, 0xFFFC)

        updateState()
    }

    private fun updateState() {
        addCpuMemoryMapping(0x6000, 0x7FFF, prgReg, PrgMemoryType.ROM)
        selectChrPage(0, chrReg)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if ((addr and 0x9000) == 0x8000 || addr >= 0xF000) {
            // A version of Bubble Bobble expects writes to $F000+ to switch the PRG banks
            chrReg = value
            prgReg = value
        } else {
            // For ProWres
            chrReg = value and 0x01
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
        s.write("chrReg", chrReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg", 0xFF)
        chrReg = s.readInt("chrReg")

        updateState()
    }
}
