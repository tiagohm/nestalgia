package br.tiagohm.nestalgia.core

import java.io.IOException
import kotlin.math.log

data class GameInfo(
    val crc: Long,
    val system: GameSystem,
    val board: String,
    val pcb: String,
    val chip: String,
    val mapperId: Int,
    val prgRomSize: Int,
    val chrRomSize: Int,
    val chrRamSize: Int,
    val workRamSize: Int,
    val saveRamSize: Int,
    val hasBattery: Boolean,
    val mirroring: MirroringType?,
    val inputType: GameInputType,
    val busConflict: BusConflictType,
    val subMapperId: Int,
    val vsType: VsSystemType,
    val vsPpuModel: PpuModel,
) {

    fun update(data: RomData, forHeaderlessRom: Boolean): RomData {
        // Boards marked as UNK should only be used for headerless roms (since their data is unverified)
        if (!forHeaderlessRom && board == "UNK") return data

        val info = data.info.copy(
            mapperId = mapperId,
            subMapperId = subMapperId,
            system = system,
            vsType = if (system == GameSystem.VS_SYSTEM) vsType else data.info.vsType,
            vsPpuModel = if (system == GameSystem.VS_SYSTEM) vsPpuModel else data.info.vsPpuModel,
            inputType = inputType,
            busConflict = busConflict,
            hasBattery = data.info.hasBattery || hasBattery,
            mirroring = mirroring ?: data.info.mirroring
        )

        return data.copy(
            info = info,
            chrRamSize = if (chrRamSize > 0) chrRamSize else data.chrRamSize,
            workRamSize = if (workRamSize > 0) workRamSize else data.workRamSize,
            saveRamSize = if (saveRamSize > 0) saveRamSize else data.saveRamSize,
        )
    }

    val nesHeader: NesHeader by lazy {
        val prgCount: UByte
        val chrCount: UByte
        var byte6 = ((mapperId and 0x0F) shl 4).toUByte()
        var byte7 = (mapperId and 0xF0).toUByte()
        val byte8 = (((subMapperId and 0x0F) shl 4) or ((mapperId and 0xF00) shr 8)).toUByte()
        var byte9: UByte = 0U
        var byte10: UByte = 0U
        var byte11: UByte = 0U
        val byte12: UByte = if (system == GameSystem.PAL) 0x01U else 0x00U
        val byte13: UByte = 0U // VS PPU variant

        if (prgRomSize > 4096 * 1024) {
            val prgSize = prgRomSize / 0x4000
            prgCount = prgSize.toUByte()
            byte9 = byte9 or ((prgSize and 0xF00) shr 8).toUByte()
        } else {
            prgCount = (prgRomSize / 0x4000).toUByte()
        }

        if (chrRomSize > 2048 * 1024) {
            val chrSize = chrRomSize / 0x2000
            chrCount = chrSize.toUByte()
            byte9 = byte9 or ((chrSize and 0xF00) shr 4).toUByte()
        } else {
            chrCount = (chrRomSize / 0x2000).toUByte()
        }

        if (hasBattery) byte6 = byte6 or 0x02U

        if (mirroring == MirroringType.VERTICAL) byte6 = byte6 or 0x01U

        if (system == GameSystem.PLAY_CHOICE) {
            byte7 = byte7 or 0x02U
        } else if (system === GameSystem.VS_SYSTEM) {
            byte7 = byte7 or 0x01U
        }

        // Don't set this, otherwise the header will be used over the game DB data
        // byte7 = byte7 or 0x08U // NES 2.0 marker

        if (saveRamSize > 0) byte10 = byte10 or ((log(saveRamSize.toDouble(), 2.0).toInt() - 6) shl 4).toUByte()

        if (workRamSize > 0) byte10 = byte10 or (log(workRamSize.toDouble(), 2.0).toInt() - 6).toUByte()

        if (chrRamSize > 0) byte11 = byte11 or (log(chrRamSize.toDouble(), 2.0).toInt() - 6).toUByte()

        NesHeader(
            "NES\u001A",
            prgCount,
            chrCount,
            byte6,
            byte7,
            byte8,
            byte9,
            byte10,
            byte11,
            byte12,
            byte13,
        )
    }

    companion object {

        @JvmStatic
        fun parse(line: String): GameInfo {
            val parts = line.split(";")

            val crc = parts[0].toLong(16)
            val system = when (parts[1]) {
                "Ntsc" -> GameSystem.NTSC
                "Pal" -> GameSystem.PAL
                "Dendy" -> GameSystem.DENDY
                "Famicom" -> GameSystem.FAMICOM
                "VsSystem" -> GameSystem.VS_SYSTEM
                "Playchoice" -> GameSystem.PLAY_CHOICE
                "VT02", "VT03", "VT09", "VT32", "VT369", "FamicloneDecimal",
                "UM6578", "VT01RedCyan" -> GameSystem.NTSC
                else -> throw IOException("Invalid system: ${parts[1]}")
            }
            val board = parts[2]
            val pcb = parts[3]
            val chip = parts[4]
            var mapper = parts[5].toInt()
            val prgRomSize = parts[6].toInt() * 1024
            val chrRomSize = (parts[7].toIntOrNull() ?: 0) * 1024
            val chrRamSize = (parts[8].toIntOrNull() ?: 0) * 1024
            val workRamSize = parts[9].toInt() * 1024
            val saveRamSize = parts[10].toInt() * 1024
            val battery = parts[11] == "1"
            val mirroring = when (parts[12]) {
                "h" -> MirroringType.HORIZONTAL
                "v" -> MirroringType.VERTICAL
                "4" -> MirroringType.FOUR_SCREENS
                "0" -> MirroringType.SCREEN_A_ONLY
                "1" -> MirroringType.SCREEN_B_ONLY
                "" -> null
                else -> throw IOException("Invalid mirroring type: ${parts[12]}")
            }
            val controller = when (val id = parts[13].toIntOrNull() ?: 0) {
                in 0..0x2D -> GameInputType.values()[id]
                else -> GameInputType.UNSPECIFIED
            }
            val busConflict = when (parts[14]) {
                "Y" -> BusConflictType.YES
                "N" -> BusConflictType.NO
                else -> BusConflictType.DEFAULT
            }
            val subMapper = parts[15].toIntOrNull() ?: -1
            val vsSystem = VsSystemType.values()[parts[16].toIntOrNull() ?: 0]
            val ppuModel = PpuModel.values()[parts[17].toIntOrNull()?.let { if (it > 10) 0 else it } ?: 0]

            if (mapper == 65000) {
                mapper = UnifLoader.getMapperId(board)
            }

            return GameInfo(
                crc,
                system,
                board,
                pcb,
                chip,
                mapper,
                prgRomSize,
                chrRomSize,
                chrRamSize,
                workRamSize,
                saveRamSize,
                battery,
                mirroring,
                controller,
                busConflict,
                subMapper,
                vsSystem,
                ppuModel
            )
        }
    }
}
