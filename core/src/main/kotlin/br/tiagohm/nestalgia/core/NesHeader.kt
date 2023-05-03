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
    val nes: String = "",
    val prgCount: UByte = 0U,
    val chrCount: UByte = 0U,
    val byte6: UByte = 0U,
    val byte7: UByte = 0U,
    val byte8: UByte = 0U,
    val byte9: UByte = 0U,
    val byte10: UByte = 0U,
    val byte11: UByte = 0U,
    val byte12: UByte = 0U,
    val byte13: UByte = 0U,
    val byte14: UByte = 0U,
    val byte15: UByte = 0U,
) {

    val hasBattery: Boolean
        get() = byte6.bit1 // 0x02

    val hasTrainer: Boolean
        get() = byte6.bit2 // 0x04

    val romHeaderVersion: RomHeaderVersion
        get() = when (byte7.toInt() and 0x0C) {
            0x08 -> RomHeaderVersion.NES20
            0x00 -> RomHeaderVersion.INES
            else -> RomHeaderVersion.OLD_INES
        }

    val mapperId: Int
        get() {
            return when (romHeaderVersion) {
                RomHeaderVersion.NES20 -> ((byte8.toInt() and 0x0F) shl 8) or (byte7.toInt() and 0xF0) or (byte6.toInt() shr 4)
                RomHeaderVersion.INES -> (byte7.toInt() and 0xF0) or (byte6.toInt() shr 4)
                RomHeaderVersion.OLD_INES -> byte6.toInt() shr 4
            }
        }

    val subMapperId: Int
        get() = if (romHeaderVersion == RomHeaderVersion.NES20) (byte8.toInt() and 0xF0) shr 4 else 0

    val mirroringType: MirroringType
        get() {
            return when {
                byte6.bit3 -> MirroringType.FOUR_SCREENS
                byte6.bit0 -> MirroringType.VERTICAL
                else -> MirroringType.HORIZONTAL
            }
        }

    val inputType: GameInputType
        get() {
            if (romHeaderVersion == RomHeaderVersion.NES20) {
                if (byte15 <= GameInputType.UFORCE.ordinal.toUByte()) {
                    return GameInputType.values()[byte15.toInt()]
                }
            }

            return GameInputType.UNSPECIFIED
        }

    val vsSystemType: VsSystemType
        get() {
            if (romHeaderVersion == RomHeaderVersion.NES20) {
                if ((byte13.toInt() shr 4) <= 0x06) {
                    return VsSystemType.values()[(byte13.toInt() shr 4)]
                }
            }

            return VsSystemType.DEFAULT
        }

    val vsPpuModel: PpuModel
        get() {
            if (romHeaderVersion == RomHeaderVersion.NES20) {
                when (byte13.toInt() and 0x0F) {
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


    val nesSystem: GameSystem
        get() {
            return when (romHeaderVersion) {
                RomHeaderVersion.NES20 -> {
                    when (byte12.toInt() and 0x03) {
                        0 -> GameSystem.NTSC
                        1 -> GameSystem.PAL
                        2 -> GameSystem.NTSC // Game works with both NTSC/PAL, pick NTSC by default
                        else -> GameSystem.DENDY
                    }
                }
                RomHeaderVersion.INES -> {
                    if (byte9.bit0) GameSystem.PAL else GameSystem.UNKNOWN
                }
                else -> {
                    GameSystem.UNKNOWN
                }
            }
        }

    val system: GameSystem
        get() {
            return when (romHeaderVersion) {
                RomHeaderVersion.NES20 -> {
                    when (byte7.toInt() and 0x03) {
                        0 -> nesSystem
                        1 -> GameSystem.VS_SYSTEM
                        2 -> GameSystem.PLAY_CHOICE
                        else -> when (byte13.toInt()) {
                            0 -> nesSystem
                            1 -> GameSystem.VS_SYSTEM
                            2 -> GameSystem.PLAY_CHOICE
                            else -> GameSystem.NTSC
                        }
                    }
                }
                RomHeaderVersion.INES -> {
                    when {
                        byte7.bit0 -> GameSystem.VS_SYSTEM
                        byte7.bit1 -> GameSystem.PLAY_CHOICE
                        else -> nesSystem
                    }
                }
                else -> {
                    nesSystem
                }
            }
        }

    private fun computeSizeValue(exponent: UInt, multiplier: UInt): UInt {
        val e = min(60U, exponent)
        val m = multiplier * 2U + 1U
        return m * (1U shl e.toInt())
    }

    val prgSize: UInt
        get() {
            return if (romHeaderVersion == RomHeaderVersion.NES20) {
                if ((byte9.toInt() and 0x0F) == 0x0F) {
                    computeSizeValue(prgCount.toUInt() shr 2, prgCount.toUInt() and 0x03U)
                } else {
                    (((byte9.toUInt() and 0x0FU) shl 8) or prgCount.toUInt()) * 0x4000U
                }
            } else if (prgCount.isZero) {
                256U * 0x4000U // 0 is a special value and means 256
            } else {
                prgCount * 0x4000U
            }
        }

    val chrSize: UInt
        get() {
            return if (romHeaderVersion == RomHeaderVersion.NES20) {
                if ((byte9.toInt() and 0xF0) == 0xF0) {
                    computeSizeValue(chrCount.toUInt() shr 2, chrCount.toUInt() and 0x03U)
                } else {
                    (((byte9.toUInt() and 0xF0U) shl 4) or chrCount.toUInt()) * 0x2000U
                }
            } else {
                chrCount * 0x2000U
            }
        }

    val workRamSize: Int
        get() {
            return if (romHeaderVersion == RomHeaderVersion.NES20) {
                val value = (byte10.toInt() and 0x0F)
                if (value == 0) 0 else 128 * 2 shl (value - 1)
            } else {
                -1
            }
        }

    val saveRamSize: Int
        get() {
            return if (romHeaderVersion == RomHeaderVersion.NES20) {
                val value = (byte10.toInt() and 0xF0) shr 4
                if (value == 0) 0 else 128 * 2 shl (value - 1)
            } else {
                -1
            }
        }

    val chrRamSize: Int
        get() {
            return if (romHeaderVersion == RomHeaderVersion.NES20) {
                val value = byte11.toInt() and 0x0F
                if (value == 0) 0 else 128 * 2 shl (value - 1)
            } else {
                -1
            }
        }

    val saveChrRamSize: Int
        get() {
            return if (romHeaderVersion == RomHeaderVersion.NES20) {
                val value = (byte11.toInt() and 0xF0) shr 4
                if (value == 0) 0 else 128 * 2 shl (value - 1)
            } else {
                -1
            }
        }

    companion object {
        val EMPTY = NesHeader()
    }
}
