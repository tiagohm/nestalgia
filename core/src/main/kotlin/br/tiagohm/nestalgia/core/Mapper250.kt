package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_250

class Mapper250 : MMC3() {

    override fun writeRegister(addr: UShort, value: UByte) {
        super.writeRegister((addr and 0xE000U) or ((addr and 0x0400U) shr 10), addr.loByte)
    }
}