package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_184

class Sunsoft184(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x1000

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0x7FFF

    override fun initialize() {
        selectPrgPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectChrPage(0, value and 0x07)

        // The most significant bit of H is always set in hardware.
        selectChrPage(1, 0x80 or (value shr 4 and 0x07))
    }
}
