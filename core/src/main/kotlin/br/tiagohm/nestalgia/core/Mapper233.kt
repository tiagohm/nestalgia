package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_233

class Mapper233(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    @Volatile private var reset = 0

    override fun initialize() = Unit

    override fun reset(softReset: Boolean) {
        if (softReset) {
            reset = reset xor 0x20
        }

        writeRegister(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (value.bit5) {
            selectPrgPage(0, reset or (value and 0x1F))
            selectPrgPage(1, reset or (value and 0x1F))
        } else {
            selectPrgPage(0, reset or (value and 0x1E))
            selectPrgPage(1, reset or (value and 0x1E) + 1)
        }

        selectChrPage(0, 0)

        mirroringType = when (value shr 6 and 0x03) {
            0 -> SCREEN_A_ONLY
            1 -> VERTICAL
            2 -> HORIZONTAL
            else -> SCREEN_B_ONLY
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("reset", reset)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        reset = s.readInt("reset")
    }
}
