package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_143

class Sachen143 : NROM() {

    override val registerStartAddress = 0x4100

    override val registerEndAddress = 0x5FFF

    override val allowRegisterRead = true

    override fun readRegister(addr: Int): Int {
        return (addr.inv() and 0x3F) or 0x40
    }
}
