package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_336

class BmcK3046(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 7)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val inner = value and 0x07
        val outer = value and 0x38

        selectPrgPage(0, outer or inner)
        selectPrgPage(1, outer or 7)
    }
}
