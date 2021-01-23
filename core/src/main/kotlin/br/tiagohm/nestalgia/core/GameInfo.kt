package br.tiagohm.nestalgia.core

import java.io.IOException

@ExperimentalUnsignedTypes
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
    val vsPpuModel: PpuModel
) {

    fun update(data: RomData, forHeaderlessRom: Boolean): RomData {
        // Boards marked as UNK should only be used for headerless roms (since their data is unverified)
        if (!forHeaderlessRom && board == "UNK") return data

        val isValid = subMapperId != -1

        val info = data.info.copy(
            mapperId = mapperId,
            subMapperId = subMapperId,
            system = system,
            vsType = if (system == GameSystem.VS_SYSTEM) vsType else data.info.vsType,
            vsPpuModel = if (system == GameSystem.VS_SYSTEM) vsPpuModel else data.info.vsPpuModel,
            inputType = inputType,
            busConflict = busConflict,
            hasBattery = if (isValid) hasBattery else data.info.hasBattery || hasBattery,
            mirroring = mirroring ?: data.info.mirroring
        )

        return data.copy(
            info = info,
            chrRamSize = if (isValid || chrRamSize > 0) chrRamSize else data.chrRamSize,
            workRamSize = if (isValid || workRamSize > 0) workRamSize else data.workRamSize,
            saveRamSize = if (isValid || saveRamSize > 0) saveRamSize else data.saveRamSize,
        )
    }

    companion object {
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
            val mapper = parts[5].toInt()
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