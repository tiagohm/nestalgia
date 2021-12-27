package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_173

class Txc22211c : Txc22211a() {

    override fun updateState() {
        selectPrgPage(0U, 0U)

        when {
            privateChrRomSize > 0x2000U -> {
                selectChrPage(
                    0U,
                    ((if (txChip.output.bit0) 0x01U else 0x00U) or
                            (if (txChip.y) 0x02U else 0U) or
                            (if (txChip.output.bit1) 0x04U else 0x00U)).toUShort()
                )
            }
            txChip.y -> selectChrPage(0U, 0U)
            else -> removePpuMemoryMapping(0U, 0x1FFFU)
        }
    }
}