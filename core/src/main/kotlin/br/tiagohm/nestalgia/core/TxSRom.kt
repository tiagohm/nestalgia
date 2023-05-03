package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_118

class TxSRom : MMC3() {

    override fun updateMirroring() {
        // This is disabled, 8001 writes are used to setup mirroring instead
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr.toUInt() and 0xE001U == 0x8001U) {
            val nametable = value.toInt() shr 7

            if (chrMode.isZero) {
                when (currentRegister.toInt()) {
                    0 -> {
                        setNametable(0, nametable)
                        setNametable(1, nametable)
                    }
                    1 -> {
                        setNametable(2, nametable)
                        setNametable(3, nametable)
                    }
                }
            } else {
                when (currentRegister.toInt()) {
                    2 -> setNametable(0, nametable)
                    3 -> setNametable(1, nametable)
                    4 -> setNametable(2, nametable)
                    5 -> setNametable(3, nametable)
                }
            }
        }

        super.writeRegister(addr, value)
    }
}