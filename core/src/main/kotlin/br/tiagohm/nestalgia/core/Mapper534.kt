package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_534

class Mapper534(console: Console) : Mapper126(console) {

    override val chrOuterBank
        get() = exRegs[0] shl 4 and 0x380

    override fun writeRegister(addr: Int, value: Int) {
        if (addr in 0xC000..0xDFFF) {
            mmc3WriteRegister(addr, value xor 0xFF)
        } else {
            mmc3WriteRegister(addr, value)
        }
    }
}
