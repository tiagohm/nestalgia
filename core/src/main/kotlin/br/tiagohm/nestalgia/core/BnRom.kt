package br.tiagohm.nestalgia.core

class BnRom : Mapper() {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x000

    override fun initialize() {
        selectPrgPage(0, powerOnByte())
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        // While the original BNROM board connects only 2 bits, it is recommended that emulators
        // implement this as an 8-bit register allowing selection of up to 8 MB PRG ROM if present.
        selectPrgPage(0, value)
    }
}
