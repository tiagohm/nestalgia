package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_173

class Txc22211c(console: Console) : Txc22211a(console) {

    override fun updateState() {
        selectPrgPage(0, 0)

        when {
            mChrRomSize > 0x2000 -> {
                selectChrPage(
                    0,
                    (if (txChip.output.bit0) 0x01 else 0x00) or
                        (if (txChip.y) 0x02 else 0) or
                        (if (txChip.output.bit1) 0x04 else 0x00)
                )
            }
            txChip.y -> selectChrPage(0, 0)
            else -> removePpuMemoryMapping(0, 0x1FFF)
        }
    }
}
