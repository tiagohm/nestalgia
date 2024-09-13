package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_289

class Bmc60311C(console: Console) : Mapper(console) {

    private var innerPrg = 0
    private var outerPrg = 0
    private var mode = 0

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        updateState()
        selectChrPage(0, 0)
    }

    private fun updateState() {
        val page = outerPrg or (if (mode.bit2) 0 else innerPrg)

        when (mode and 0x03) {
            0 -> {
                // 0: NROM-128: Same inner/outer 16 KiB bank at CPU $8000-$BFFF and $C000-$FFFF
                selectPrgPage(0, page)
                selectPrgPage(1, page)
            }
            1 -> {
                // 1: NROM-256: 32 kiB bank at CPU $8000-$FFFF (Selected inner/outer bank SHR 1)
                selectPrgPage2x(0, page and 0xFE)
            }
            2 -> {
                // 2: UNROM: Inner/outer bank at CPU $8000-BFFF, fixed inner bank 7 within outer bank at $C000-$FFFF
                selectPrgPage(0, page)
                selectPrgPage(1, outerPrg or 7)
            }
        }

        mirroringType = if (mode.bit4) MirroringType.HORIZONTAL else MirroringType.VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr >= 0x8000) {
            innerPrg = value and 0x07
            updateState()
        } else {
            when (addr and 0xE001) {
                0x6000 -> {
                    mode = value and 0x0F
                    updateState()
                }
                0x6001 -> {
                    outerPrg = value
                    updateState()
                }
            }
        }
    }
}
