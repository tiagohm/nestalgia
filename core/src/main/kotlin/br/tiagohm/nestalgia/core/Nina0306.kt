package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_079
// https://wiki.nesdev.com/w/index.php/INES_Mapper_113
// https://wiki.nesdev.com/w/index.php/INES_Mapper_146

@ExperimentalUnsignedTypes
class Nina0306(val multicartMode: Boolean) : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x4100U

    override val registerEndAddress: UShort = 0x5FFFU

    override fun init() {
        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if ((addr.toInt() and 0xE100) == 0x4100) {
            val v = value.toUShort()

            if (multicartMode) {
                // Mapper 113
                selectPrgPage(0U, (v shr 3) and 0x07U)
                selectChrPage(0U, (v and 0x07U) or ((v shr 3) and 0x08U))
                mirroringType = if (value.bit7) MirroringType.VERTICAL else MirroringType.HORIZONTAL
            } else {
                selectPrgPage(0U, (v shr 3) and 0x01U)
                selectChrPage(0U, v and 0x07U)
            }
        }
    }
}