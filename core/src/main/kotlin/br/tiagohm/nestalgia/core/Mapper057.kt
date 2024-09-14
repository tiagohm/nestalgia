package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_057

class Mapper057(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    private val registers = IntArray(2)

    override fun initialize() {
        updateState()
    }

    private fun updateState() {
        mirroringType = if (registers[1].bit3) HORIZONTAL else VERTICAL

        selectChrPage(0, registers[0] and 0x40 shr 3 or (registers[0] or registers[1] and 0x07))

        if (registers[1].bit4) {
            selectPrgPage(0, registers[1] shr 5 and 0x06)
            selectPrgPage(1, (registers[1] shr 5 and 0x06) + 1)
        } else {
            selectPrgPage(0, registers[1] shr 5 and 0x07)
            selectPrgPage(1, registers[1] shr 5 and 0x07)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0x8800) {
            0x8000 -> registers[0] = value
            0x8800 -> registers[1] = value
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("registers", registers)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("registers", registers)
    }
}
