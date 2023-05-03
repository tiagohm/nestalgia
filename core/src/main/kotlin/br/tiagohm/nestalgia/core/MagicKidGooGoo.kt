package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_190

class MagicKidGooGoo : Mapper() {

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x800U

    override fun init() {
        selectPrgPage(0U, 0U)
        selectPrgPage(1U, 0U)

        selectChrPage4x(0U, 0U)

        mirroringType = MirroringType.VERTICAL
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        when {
            addr in 0x8000U..0x9FFFU -> selectPrgPage(0U, (value and 0x07U).toUShort())
            addr in 0xC000U..0xDFFFU -> selectPrgPage(0U, ((value and 0x07U) or 0x08U).toUShort())
            addr.toUInt() and 0xA000U == 0xA000U -> selectChrPage(addr and 0x03U, value.toUShort())
        }
    }
}