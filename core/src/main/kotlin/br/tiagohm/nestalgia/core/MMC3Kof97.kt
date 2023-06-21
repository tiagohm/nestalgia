package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_263

class MMC3Kof97(console: Console) : MMC3(console) {

    override fun writeRegister(addr: Int, value: Int) {
        val newValue = value and 0xD8 or (value and 0x20 shr 4) or (value and 0x04 shl 3) or (value and 0x02 shr 1) or (value and 0x01 shl 2)

        super.writeRegister(
            when (addr) {
                0x9000 -> 0x8001
                0xB000 -> 0xA001
                0xD000 -> 0xC001
                0xF000 -> 0xE001
                else -> addr
            }, newValue
        )
    }
}
