package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_255

class Bmc255(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        writeRegister(0x8000, 0x00)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val prgBit = if (addr and 0x1000 != 0) 0 else 1
        val bank = (addr shr 8 and 0x40) or (addr shr 6 and 0x3F)

        selectPrgPage(0, bank and prgBit.inv())
        selectPrgPage(1, bank or prgBit)
        selectChrPage(0, (addr shr 8 and 0x40) or (addr and 0x3F))

        mirroringType = if (addr and 0x2000 != 0) MirroringType.HORIZONTAL
        else MirroringType.VERTICAL
    }
}
