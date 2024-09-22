package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_277

class Mapper277(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    @Volatile private var locked = false

    override fun initialize() {
        reset()
    }

    override fun reset(softReset: Boolean) {
        locked = false
        writeRegister(0, 0x08)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (!locked) {
            val prgBank = value and 0x0F

            locked = value.bit5

            if (value.bit3) {
                if (value.bit0) {
                    selectPrgPage(0, prgBank)
                    selectPrgPage(1, prgBank)
                } else {
                    selectPrgPage2x(0, prgBank and 0xFE)
                }
            } else {
                selectPrgPage(0, prgBank)
                selectPrgPage(1, prgBank or 0x07)
            }

            selectChrPage(0, 0)
            mirroringType = if (value.bit4) HORIZONTAL else VERTICAL
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("locked", locked)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        locked = s.readBoolean("locked")
    }
}
