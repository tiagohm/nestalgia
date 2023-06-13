package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_015

class Mapper015(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectChrPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        mirroringType = if (value.bit6) HORIZONTAL else VERTICAL

        val subBank = value shr 7
        var bank = value and 0x7F shl 1
        val mode = addr and 0x03

        addPpuMemoryMapping(0, 0x1FFF, 0, DEFAULT, if (mode == 0 || mode == 3) READ else READ_WRITE)

        when (mode) {
            0 -> {
                selectPrgPage(0, bank xor subBank)
                selectPrgPage(1, bank + 1 xor subBank)
                selectPrgPage(2, bank + 2 xor subBank)
                selectPrgPage(3, bank + 3 xor subBank)
            }
            1,
            3 -> {
                bank = bank or subBank
                selectPrgPage(0, bank)
                selectPrgPage(1, bank + 1)
                bank = (if (mode == 3) bank else bank or 0x0E) or subBank
                selectPrgPage(2, bank + 0)
                selectPrgPage(3, bank + 1)
            }
            2 -> {
                bank = bank or subBank
                selectPrgPage(0, bank)
                selectPrgPage(1, bank)
                selectPrgPage(2, bank)
                selectPrgPage(3, bank)
            }
        }
    }
}
