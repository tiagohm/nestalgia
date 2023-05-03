package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_007

class AXROM : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val hasBusConflicts: Boolean
        get() = info.subMapperId == 2

    override fun init() {
        selectChrPage(0U, 0U)
        writeRegister(0U, getPowerOnByte())
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        selectPrgPage(0U, value.toUShort() and 0x0FU)

        mirroringType = if (value.bit4) MirroringType.SCREEN_B_ONLY else MirroringType.SCREEN_A_ONLY
    }
}