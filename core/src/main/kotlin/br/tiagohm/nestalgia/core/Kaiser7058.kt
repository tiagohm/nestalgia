package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_171

class Kaiser7058(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x1000

    override val registerStartAddress = 0xF000

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        selectPrgPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF080) {
            0xF000 -> selectChrPage(0, value)
            0xF080 -> selectChrPage(1, value)
        }
    }
}
