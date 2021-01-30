package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_118

@ExperimentalUnsignedTypes
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
                        setNametable(0U, nametable)
                        setNametable(1U, nametable)
                    }
                    1 -> {
                        setNametable(2U, nametable)
                        setNametable(3U, nametable)
                    }
                }
            } else {
                when (currentRegister.toInt()) {
                    2 -> setNametable(0U, nametable)
                    3 -> setNametable(1U, nametable)
                    4 -> setNametable(2U, nametable)
                    5 -> setNametable(3U, nametable)
                }
            }
        }

        super.writeRegister(addr, value)
    }
}