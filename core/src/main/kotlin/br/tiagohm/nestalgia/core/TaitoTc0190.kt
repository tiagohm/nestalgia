package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_033

class TaitoTc0190(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override fun initialize() {
        selectPrgPage(2, -2)
        selectPrgPage(3, -1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xA003) {
            0x8000 -> {
                selectPrgPage(0, value and 0x3F)
                mirroringType = if (value.bit6) HORIZONTAL else VERTICAL
            }
            0x8001 -> selectPrgPage(1, value and 0x3F)
            0x8002 -> {
                selectChrPage(0, value * 2)
                selectChrPage(1, value * 2 + 1)
            }
            0x8003 -> {
                selectChrPage(2, value * 2)
                selectChrPage(3, value * 2 + 1)
            }
            0xA000, 0xA001, 0xA002, 0xA003 -> selectChrPage(4 + (addr and 0x03), value)
        }
    }
}
