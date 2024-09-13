package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_063

class Bmc63(console: Console) : Mapper(console) {

    @Volatile private var openBus = false

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0)
    }

    override fun reset(softReset: Boolean) {
        openBus = false
    }

    override fun writeRegister(addr: Int, value: Int) {
        val b = addr shr 1 and 0x1FC
        val c = addr shr 1 and 0x02

        val bit1 = addr.bit1

        openBus = (addr and 0x0300) == 0x0300

        if (openBus) {
            removeCpuMemoryMapping(0x8000, 0xBFFF)
        } else {
            selectPrgPage(0, b or if (bit1) 0 else (c or 0))
            selectPrgPage(1, b or if (bit1) 1 else (c or 1))
        }

        selectPrgPage(2, b or if (bit1) 2 else (c or 0))
        selectPrgPage(
            3,
            if (addr and 0x800 != 0) (addr and 0x07C) or (if (addr and 0x06 != 0) 3 else 1)
            else b or if (bit1) 3 else (c or 1)
        )

        mirroringType = if (addr.bit0) MirroringType.HORIZONTAL else MirroringType.VERTICAL
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("openBus", openBus)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        openBus = s.readBoolean("openBus")

        if (openBus) {
            removeCpuMemoryMapping(0x8000, 0xBFFF)
        }
    }
}
