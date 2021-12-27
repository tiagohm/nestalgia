package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_063

class Bmc63 : Mapper() {

    private var openBus = false

    override val prgPageSize = 0x2000U

    override val chrPageSize = 0x2000U

    override fun init() {
        writeRegister(0x8000U, 0U)
    }

    override fun reset(softReset: Boolean) {
        openBus = false
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        val a = addr.toInt()
        val b = a shr 1 and 0x1FC
        val c = a shr 1 and 0x02

        val bit1 = a and 0x2 != 0

        openBus = (a and 0x0300) == 0x0300

        if (openBus) {
            removeCpuMemoryMapping(0x8000U, 0xBFFFU)
        } else {
            selectPrgPage(0U, (b or if (bit1) 0 else (c or 0)).toUShort())
            selectPrgPage(1U, (b or if (bit1) 1 else (c or 1)).toUShort())
        }

        selectPrgPage(2U, (b or if (bit1) 2 else (c or 0)).toUShort())
        selectPrgPage(
            3U,
            (if (a and 0x800 != 0) (a and 0x07C) or if (a and 0x06 != 0) 3 else 1
            else b or if (bit1) 3 else c or 1).toUShort()
        )

        mirroringType = if (a and 1 != 0) MirroringType.HORIZONTAL else MirroringType.VERTICAL
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("openBus", openBus)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        openBus = s.readBoolean("openBus") ?: false

        if (openBus) {
            removeCpuMemoryMapping(0x8000U, 0xBFFFU)
        }
    }
}