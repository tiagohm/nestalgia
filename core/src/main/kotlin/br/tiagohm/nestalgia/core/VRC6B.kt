package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_026

class VRC6B(console: Console) : VRC6(console) {

    override fun writeRegister(addr: Int, value: Int) {
        super.writeRegister(addr and 0xFFFC or (addr and 0x01 shl 1) or (addr and 0x02 shr 1), value)
    }
}
