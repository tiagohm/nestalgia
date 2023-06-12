package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_218

class MagicFloor218 : Mapper() {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)

        if (mirroringType == FOUR_SCREENS) {
            mirroringType = if (info.header.byte6.bit0) SCREEN_B_ONLY else SCREEN_A_ONLY
        }

        val mask = when (mirroringType) {
            VERTICAL -> 0x400
            HORIZONTAL -> 0x800
            SCREEN_A_ONLY -> 0x1000
            SCREEN_B_ONLY -> 0x2000
            else -> 0
        }

        repeat(8) {
            addPpuMemoryMapping(it * 0x400, it * 0x400 + 0x3FF, if (it * 0x400 and mask != 0) 1 else 0, NAMETABLE_RAM)
        }
    }
}
