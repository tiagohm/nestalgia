package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_177

class Henggedianzi177(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value)

        mirroringType = if (value.bit5) MirroringType.HORIZONTAL
        else MirroringType.VERTICAL
    }
}
