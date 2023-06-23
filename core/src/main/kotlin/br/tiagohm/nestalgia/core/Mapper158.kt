package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_158

class Mapper158(console: Console) : Rambo1(console) {

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0xE001 == 0x8001) {
            val nametable = value shr 7

            if (currentRegister.bit7) {
                when (currentRegister and 0x07) {
                    2 -> nametable(0, nametable)
                    3 -> nametable(1, nametable)
                    4 -> nametable(2, nametable)
                    5 -> nametable(3, nametable)
                }
            } else {
                when (currentRegister and 0x07) {
                    0 -> {
                        nametable(0, nametable)
                        nametable(1, nametable)
                    }
                    1 -> {
                        nametable(2, nametable)
                        nametable(3, nametable)
                    }
                }
            }
        }

        if (addr and 0xE001 != 0xA000) {
            super.writeRegister(addr, value)
        }
    }
}
