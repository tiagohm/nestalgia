package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_079
// https://wiki.nesdev.com/w/index.php/INES_Mapper_113
// https://wiki.nesdev.com/w/index.php/INES_Mapper_146

class Nina0306(private val multicartMode: Boolean) : Mapper() {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x4100

    override val registerEndAddress = 0x5FFF

    override fun initialize() {
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if ((addr and 0xE100) == 0x4100) {
            if (multicartMode) {
                // Mapper 113.
                selectPrgPage(0, value shr 3 and 0x07)
                selectChrPage(0, (value and 0x07) or (value shr 3 and 0x08))

                mirroringType = if (value.bit7) MirroringType.VERTICAL
                else MirroringType.HORIZONTAL
            } else {
                selectPrgPage(0, value shr 3 and 0x01)
                selectChrPage(0, value and 0x07)
            }
        }
    }
}
