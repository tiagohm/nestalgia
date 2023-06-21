package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_193

class NtdecTc112(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0800

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0x7FFF

    override fun initialize() {
        selectPrgPage(1, -3)
        selectPrgPage(2, -2)
        selectPrgPage(3, -1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val page = value shr 1

        when (addr and 0x03) {
            0 -> {
                selectChrPage(0, page)
                selectChrPage(1, page + 1)
            }
            1 -> selectChrPage(2, page)
            2 -> selectChrPage(3, page)
            3 -> selectPrgPage(0, value)
        }
    }
}
