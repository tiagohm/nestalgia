package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.RAM
import br.tiagohm.nestalgia.core.MirroringType.FOUR_SCREENS

// https://www.nesdev.org/wiki/NES_2.0_Mapper_356

class Mapper356(console: Console) : Mapper045(console) {

    override val chrRamPageSize = 0x400

    override val chrRamSize = 0x2000

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (!reg[2].bit5) {
            mmc3SelectChrPage(slot, slot, RAM)
        } else {
            val mask = 0xFF shr (0x0F - (reg[2] and 0x0F))
            mmc3SelectChrPage(slot, (page and mask) or (reg[0] or (reg[2] and 0xF0 shl 4)), memoryType)
        }
    }

    override fun updateMirroring() {
        if (reg[2].bit6) {
            mirroringType = FOUR_SCREENS
        } else {
            mmc3UpdateMirroring()
        }
    }
}
