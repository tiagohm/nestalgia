package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_329

class Edu2000(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val workRamSize = 0x8000

    override val workRamPageSize = 0x2000

    @Volatile private var reg = 0

    override fun initialize() {
        updatePrg()
        selectChrPage(0, 0)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("reg", reg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        reg = s.readInt("reg")

        updatePrg()
    }

    private fun updatePrg() {
        selectPrgPage(0, reg and 0x1F)
        addCpuMemoryMapping(0x6000, 0x7FFF, (reg shr 6) and 0x03, PrgMemoryType.WRAM)
    }

    override fun writeRegister(addr: Int, value: Int) {
        reg = value
        updatePrg()
    }
}
