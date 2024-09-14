package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*
import java.io.IOException
import kotlin.math.log

data class GameInfo(
    @JvmField val crc: Long,
    @JvmField val system: GameSystem,
    @JvmField val board: String,
    @JvmField val pcb: String,
    @JvmField val chip: String,
    @JvmField val mapperId: Int,
    @JvmField val prgRomSize: Int,
    @JvmField val chrRomSize: Int,
    @JvmField val chrRamSize: Int,
    @JvmField val workRamSize: Int,
    @JvmField val saveRamSize: Int,
    @JvmField val hasBattery: Boolean,
    @JvmField val mirroring: MirroringType?,
    @JvmField val inputType: GameInputType,
    @JvmField val busConflict: BusConflictType,
    @JvmField val subMapperId: Int,
    @JvmField val vsType: VsSystemType,
    @JvmField val vsPpuModel: PpuModel,
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

    val nesHeader by lazy {
        val prgCount: Int
        val chrCount: Int
        var byte6 = (mapperId and 0x0F) shl 4
        var byte7 = mapperId and 0xF0
        val byte8 = (subMapperId and 0x0F shl 4) or (mapperId and 0xF00 shr 8)
        var byte9 = 0
        var byte10 = 0
        var byte11 = 0
        val byte12 = if (system == GameSystem.PAL) 0x01 else 0x00
        val byte13 = 0 // VS PPU variant

        if (prgRomSize > 4096 * 1024) {
            val prgSize = prgRomSize / 0x4000
            prgCount = prgSize
            byte9 = prgSize and 0xF00 shr 8
        } else {
            prgCount = prgRomSize / 0x4000
        }

        if (chrRomSize > 2048 * 1024) {
            val chrSize = chrRomSize / 0x2000
            chrCount = chrSize
            byte9 = byte9 or chrSize and 0xF00 shr 4
        } else {
            chrCount = chrRomSize / 0x2000
        }

        if (hasBattery) byte6 = byte6 or 0x02

        if (mirroring == VERTICAL) byte6 = byte6 or 0x01

        if (system == GameSystem.PLAY_CHOICE) {
            byte7 = byte7 or 0x02
        } else if (system === GameSystem.VS_SYSTEM) {
            byte7 = byte7 or 0x01
        }

        // Don't set this, otherwise the header will be used over the game DB data
        // byte7 = byte7 or 0x08U // NES 2.0 marker

        if (saveRamSize > 0) byte10 = byte10 or ((log(saveRamSize.toDouble(), 2.0).toInt() - 6) shl 4)
        if (workRamSize > 0) byte10 = byte10 or (log(workRamSize.toDouble(), 2.0).toInt() - 6)
        if (chrRamSize > 0) byte11 = byte11 or (log(chrRamSize.toDouble(), 2.0).toInt() - 6)

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

        fun parse(line: String): GameInfo {
            val parts = line.split(";", ",")

            val crc = parts[0].toLong(16)
            val system = when (parts[1]) {
                "NesNtsc" -> GameSystem.NTSC
                "NesPal" -> GameSystem.PAL
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
                "h" -> HORIZONTAL
                "v" -> VERTICAL
                "4" -> FOUR_SCREENS
                "0" -> SCREEN_A_ONLY
                "1" -> SCREEN_B_ONLY
                "" -> null
                else -> throw IOException("Invalid mirroring type: ${parts[12]}")
            }
            val controller = when (val id = parts[13].toIntOrNull() ?: 0) {
                in 0..0x2D -> GameInputType.entries[id]
                else -> GameInputType.UNSPECIFIED
            }
            val busConflict = when (parts[14]) {
                "Y" -> BusConflictType.YES
                "N" -> BusConflictType.NO
                else -> BusConflictType.DEFAULT
            }
            val subMapper = parts[15].toIntOrNull() ?: -1
            val vsSystem = VsSystemType.entries[parts[16].toIntOrNull() ?: 0]
            val ppuModel = PpuModel.entries[parts[17].toIntOrNull()?.let { if (it > 10) 0 else it } ?: 0]

            if (mapper == 65000) {
                mapper = UnifLoader.mapperId(board)
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
                ppuModel,
            )
        }
    }
}
