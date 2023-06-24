package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_346

class Kaiser7012(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 1)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0xE0A0 -> selectPrgPage(0, 0)
            0xEE36 -> selectPrgPage(0, 1)
        }
    }
}
