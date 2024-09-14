package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://www.nesdev.org/wiki/NES_2.0_Mapper_530

class Ax5705(console: Console) : Mapper(console) {

    private val chrReg = IntArray(8)

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override fun initialize() {
        selectPrgPage(2, -2)
        selectPrgPage(3, -1)

        repeat(8) {
            selectChrPage(it, chrReg[it])
        }
    }

    private fun updateChrReg(index: Int, value: Int, low: Boolean) {
        if (low) {
            chrReg[index] = chrReg[index] and 0xF0 or (value and 0x0F)
        } else {
            chrReg[index] = chrReg[index] and 0x0F or (value and 0x04 shr 1 or (value and 0x02 shl 1) or (value and 0x09) shl 4)
        }

        selectChrPage(index, chrReg[index])
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr >= 0xA008) {
            val low = !addr.bit0

            when (addr and 0xF00E) {
                0xA008 -> updateChrReg(0, value, low)
                0xA00A -> updateChrReg(1, value, low)
                0xC000 -> updateChrReg(2, value, low)
                0xC002 -> updateChrReg(3, value, low)
                0xC008 -> updateChrReg(4, value, low)
                0xC00A -> updateChrReg(5, value, low)
                0xE000 -> updateChrReg(6, value, low)
                0xE002 -> updateChrReg(7, value, low)
            }
        } else {
            when (addr and 0xF00F) {
                0x8000 -> selectPrgPage(0, value and 0x02 shl 2 or (value and 0x08 shr 2) or (value and 0x05))
                0x8008 -> mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
                0xA000 -> selectPrgPage(1, value and 0x02 shl 2 or (value and 0x08 shr 2) or (value and 0x05))
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chrReg", chrReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("chrReg", chrReg)
    }
}
