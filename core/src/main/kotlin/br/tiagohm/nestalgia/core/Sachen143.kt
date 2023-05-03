package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_143

class Sachen143 : NROM() {

    override val registerStartAddress: UShort = 0x4100U

    override val registerEndAddress: UShort = 0x5FFFU

    override val allowRegisterRead = true

    override fun readRegister(addr: UShort): UByte {
        return ((addr.inv() and 0x3FU) or 0x40U).toUByte()
    }
}