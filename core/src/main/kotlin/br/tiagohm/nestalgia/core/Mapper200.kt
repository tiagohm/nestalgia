package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Mapper200 : Mapper() {

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x2000U

    override fun init() {
        setBank(0U)
    }

    private inline fun setBank(bank: UShort) {
        selectPrgPage(0U, bank)
        selectPrgPage(1U, bank)
        selectChrPage(0U, bank)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        setBank(addr and 0x07U)
        mirroringType = if (value.bit3) MirroringType.VERTICAL else MirroringType.HORIZONTAL
    }
}