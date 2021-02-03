package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class BnRom : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x000U

    override fun init() {
        selectPrgPage(0U, getPowerOnByte().toUShort())
        selectChrPage(0U, 0U)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        // While the original BNROM board connects only 2 bits, it is recommended that emulators
        // implement this as an 8-bit register allowing selection of up to 8 MB PRG ROM if present.
        selectPrgPage(0U, value.toUShort())
    }
}