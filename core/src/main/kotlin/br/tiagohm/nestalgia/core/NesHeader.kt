package br.tiagohm.nestalgia.core

import kotlin.math.min

// https://wiki.nesdev.com/w/index.php/INES
// https://wiki.nesdev.com/w/index.php/NES_2.0

data class NesHeader(
    /*
	Thing 	    Archaic			 	iNES 								NES 2.0
	Byte 6	    Mapper low nibble,  Mirroring, Battery/Trainer flags
	Byte 7 	    Unused 				Mapper high nibble, Vs. 		    Mapper high nibble, NES 2.0 signature, PlayChoice, Vs.
	Byte 8 	    Unused 				Total PRG RAM size (linear) 	    Mapper highest nibble, mapper variant
	Byte 9 	    Unused 				TV system 							Upper bits of ROM size
	Byte 10 	Unused 				Unused 								PRG RAM size (logarithmic; battery and non-battery)
	Byte 11 	Unused 				Unused 								VRAM size (logarithmic; battery and non-battery)
	Byte 12 	Unused 				Unused 								TV system
	Byte 13 	Unused 				Unused 								Vs. PPU variant
	*/
    @JvmField val nes: String = "",
    @JvmField val prgCount: Int = 0,
    @JvmField val chrCount: Int = 0,
    @JvmField val byte6: Int = 0,
    @JvmField val byte7: Int = 0,
    @JvmField val byte8: Int = 0,
    @JvmField val byte9: Int = 0,
    @JvmField val byte10: Int = 0,
    @JvmField val byte11: Int = 0,
    @JvmField val byte12: Int = 0,
    @JvmField val byte13: Int = 0,
    @JvmField val byte14: Int = 0,
    @JvmField val byte15: Int = 0,
) {

    val hasBattery
        get() = byte6.bit1 // 0x02

    val hasTrainer
        get() = byte6.bit2 // 0x04

    val romHeaderVersion
        get() = when (byte7 and 0x0C) {
            0x08 -> RomHeaderVersion.NES20
            0x00 -> RomHeaderVersion.INES
            else -> RomHeaderVersion.OLD_INES
        }

    val mapperId
        get() = when (romHeaderVersion) {
            RomHeaderVersion.NES20 -> ((byte8 and 0x0F) shl 8) or (byte7 and 0xF0) or (byte6 shr 4)
            RomHeaderVersion.INES -> (byte7 and 0xF0) or (byte6 shr 4)
            RomHeaderVersion.OLD_INES -> byte6 shr 4
        }

    val subMapperId
        get() = if (romHeaderVersion == RomHeaderVersion.NES20) (byte8 and 0xF0) shr 4 else 0

    val mirroringType
        get() = when {
            byte6.bit3 -> MirroringType.FOUR_SCREENS
            byte6.bit0 -> MirroringType.VERTICAL
            else -> MirroringType.HORIZONTAL
        }

    val inputType: GameInputType
        get() {
            if (romHeaderVersion == RomHeaderVersion.NES20) {
                if (byte15 <= GameInputType.UFORCE.ordinal) {
                    return GameInputType.entries[byte15]
                }
            }

            return GameInputType.UNSPECIFIED
        }

    val vsSystemType: VsSystemType
        get() {
            if (romHeaderVersion == RomHeaderVersion.NES20) {
                if ((byte13 shr 4) <= 0x06) {
                    return VsSystemType.entries[(byte13 shr 4)]
                }
            }

            return VsSystemType.DEFAULT
        }

    val vsPpuModel: PpuModel
        get() {
            if (romHeaderVersion == RomHeaderVersion.NES20) {
                when (byte13 and 0x0F) {
                    0 -> return PpuModel.PPU_2C03
                    // Unsupported VS System Palette specified (2C03G)
                    1 -> return PpuModel.PPU_2C03
                    2 -> return PpuModel.PPU_2C04A
                    3 -> return PpuModel.PPU_2C04B
                    4 -> return PpuModel.PPU_2C04C
                    5 -> return PpuModel.PPU_2C04D
                    6 -> return PpuModel.PPU_2C03
                    7 -> return PpuModel.PPU_2C03
                    8 -> return PpuModel.PPU_2C05A
                    9 -> return PpuModel.PPU_2C05B
                    10 -> return PpuModel.PPU_2C05C
                    11 -> return PpuModel.PPU_2C05D
                    12 -> return PpuModel.PPU_2C05E
                }
            }

            return PpuModel.PPU_2C03
        }


    val nesSystem
        get() = when (romHeaderVersion) {
            RomHeaderVersion.NES20 -> when (byte12 and 0x03) {
                0 -> GameSystem.NTSC
                1 -> GameSystem.PAL
                2 -> GameSystem.NTSC // Game works with both NTSC/PAL, pick NTSC by default
                else -> GameSystem.DENDY
            }
            RomHeaderVersion.INES -> if (byte9.bit0) GameSystem.PAL else GameSystem.UNKNOWN
            else -> GameSystem.UNKNOWN
        }

    val system
        get() = when (romHeaderVersion) {
            RomHeaderVersion.NES20 -> when (byte7 and 0x03) {
                0 -> nesSystem
                1 -> GameSystem.VS_SYSTEM
                2 -> GameSystem.PLAY_CHOICE
                else -> when (byte13) {
                    0 -> nesSystem
                    1 -> GameSystem.VS_SYSTEM
                    2 -> GameSystem.PLAY_CHOICE
                    else -> GameSystem.NTSC
                }
            }
            RomHeaderVersion.INES -> when {
                byte7.bit0 -> GameSystem.VS_SYSTEM
                byte7.bit1 -> GameSystem.PLAY_CHOICE
                else -> nesSystem
            }
            else -> nesSystem
        }

    private fun computeSizeValue(exponent: Int, multiplier: Int): Int {
        val e = min(60, exponent)
        val m = multiplier * 2 + 1
        return m * (1 shl e)
    }

    val prgSize
        get() = if (romHeaderVersion == RomHeaderVersion.NES20) {
            if ((byte9 and 0x0F) == 0x0F) {
                computeSizeValue(prgCount shr 2, prgCount and 0x03)
            } else {
                (((byte9 and 0x0F) shl 8) or prgCount) * 0x4000
            }
        } else if (prgCount == 0) {
            256 * 0x4000 // 0 is a special value and means 256
        } else {
            prgCount * 0x4000
        }

    val chrSize
        get() = if (romHeaderVersion == RomHeaderVersion.NES20) {
            if ((byte9 and 0xF0) == 0xF0) {
                computeSizeValue(chrCount shr 2, chrCount and 0x03)
            } else {
                (((byte9 and 0xF0) shl 4) or chrCount) * 0x2000
            }
        } else {
            chrCount * 0x2000
        }

    val workRamSize
        get() = if (romHeaderVersion == RomHeaderVersion.NES20) {
            val value = (byte10 and 0x0F)
            if (value == 0) 0 else 128 * 2 shl (value - 1)
        } else {
            -1
        }

    val saveRamSize
        get() = if (romHeaderVersion == RomHeaderVersion.NES20) {
            val value = (byte10 and 0xF0) shr 4
            if (value == 0) 0 else 128 * 2 shl (value - 1)
        } else {
            -1
        }

    val chrRamSize
        get() = if (romHeaderVersion == RomHeaderVersion.NES20) {
            val value = byte11 and 0x0F
            if (value == 0) 0 else 128 * 2 shl (value - 1)
        } else {
            -1
        }

    val saveChrRamSize
        get() = if (romHeaderVersion == RomHeaderVersion.NES20) {
            val value = (byte11 and 0xF0) shr 4
            if (value == 0) 0 else 128 * 2 shl (value - 1)
        } else {
            -1
        }

    companion object {

        val EMPTY = NesHeader()
    }
}
