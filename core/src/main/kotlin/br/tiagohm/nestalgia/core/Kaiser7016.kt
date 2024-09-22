package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_306

class Kaiser7016(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    @Volatile private var prgReg = 8

    override fun initialize() {
        selectPrgPage(0, 0x0C)
        selectPrgPage(1, 0x0D)
        selectPrgPage(2, 0x0E)
        selectPrgPage(3, 0x0F)
        selectChrPage(0, 0)

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg")

        updateState()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateState() {
        addCpuMemoryMapping(0x6000, 0x7FFF, prgReg, PrgMemoryType.ROM)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val mode = (addr and 0x30) == 0x30

        when (addr and 0xD943) {
            0xD943 -> {
                prgReg = if (mode) 0x0B else (addr shr 2) and 0x0F
                updateState()
            }
            0xD903 -> {
                prgReg = if (mode) 0x08 or (addr shr 2 and 0x03) else 0x0B
                updateState()
            }
        }
    }
}
