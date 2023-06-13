package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_007

class AXROM(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val hasBusConflicts
        get() = info.subMapperId == 2

    override fun initialize() {
        selectChrPage(0, 0)
        writeRegister(0, powerOnByte())
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value and 0x0F)

        mirroringType = if (value.bit4) MirroringType.SCREEN_B_ONLY
        else MirroringType.SCREEN_A_ONLY
    }
}
