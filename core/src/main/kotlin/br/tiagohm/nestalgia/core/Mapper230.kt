package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_230

class Mapper230(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    @Volatile private var contraMode = false

    override fun initialize() {
        selectChrPage(0, 0)
        reset(true)
    }

    override fun reset(softReset: Boolean) {
        if (softReset) {
            contraMode = !contraMode

            mirroringType = if (contraMode) {
                selectPrgPage(0, 0)
                selectPrgPage(1, 7)
                VERTICAL
            } else {
                selectPrgPage(0, 8)
                selectPrgPage(1, 9)
                HORIZONTAL
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (contraMode) {
            selectPrgPage(0, value and 0x07)
        } else {
            if (value.bit5) {
                selectPrgPage(0, (value and 0x1F) + 8)
                selectPrgPage(1, (value and 0x1F) + 8)
            } else {
                selectPrgPage(0, (value and 0x1E) + 8)
                selectPrgPage(1, (value and 0x1E) + 9)
            }

            mirroringType = if (value.bit6) VERTICAL else HORIZONTAL
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("contraMode", contraMode)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        contraMode = s.readBoolean("contraMode")
    }
}
