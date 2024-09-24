package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.PrgMemoryType.WRAM

// https://www.nesdev.org/wiki/NES_2.0_Mapper_557

class Mapper557(console: Console) : Namco108(console) {

    override val chrPageSize = 0x2000

    override fun updatePrgMapping() {
        addCpuMemoryMapping(0x6000, 0x7FFF, 0, WRAM)

        selectPrgPage(0, registers[6] and 0x0F)
        selectPrgPage(1, registers[7] and 0x0F)
        selectPrgPage(2, (-2) and 0x0F)
        selectPrgPage(3, (-1) and 0x0F)
    }

    override fun updateChrMapping() {
        selectChrPage(0, 0)
    }

    override fun updateState() {
        mirroringType = if (registers[5].bit5) MirroringType.HORIZONTAL else MirroringType.VERTICAL
        super.updateState()
    }
}
