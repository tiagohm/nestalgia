package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_118

class TxSRom(console: Console) : MMC3(console) {

    override fun updateMirroring() {
        // This is disabled, 8001 writes are used to setup mirroring instead.
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0xE001 == 0x8001) {
            val nametable = value shr 7

            if (chrMode == 0) {
                when (currentRegister) {
                    0 -> {
                        nametable(0, nametable)
                        nametable(1, nametable)
                    }
                    1 -> {
                        nametable(2, nametable)
                        nametable(3, nametable)
                    }
                }
            } else {
                when (currentRegister) {
                    2 -> nametable(0, nametable)
                    3 -> nametable(1, nametable)
                    4 -> nametable(2, nametable)
                    5 -> nametable(3, nametable)
                }
            }
        }

        super.writeRegister(addr, value)
    }
}
