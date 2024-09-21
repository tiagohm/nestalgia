package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_324

class FaridUnrom(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val hasBusConflicts = true

    @Volatile private var reg = 0

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 7)

        selectChrPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        reg = if (softReset) reg and 0x87 else 0
    }

    override fun writeRegister(addr: Int, value: Int) {
        val locked = reg.bit3

        if (!locked && !reg.bit7 && value.bit7) {
            // Latch bits
            reg = (reg and 0x87) or (value and 0x78)
        }

        reg = (reg and 0x78) or (value and 0x87)

        val outer = reg and 0x70

        selectPrgPage(0, (reg and 0x07) or (outer shr 1))
        selectPrgPage(1, 0x07 or (outer shr 1))
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("reg", reg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        reg = s.readInt("reg")
    }
}
