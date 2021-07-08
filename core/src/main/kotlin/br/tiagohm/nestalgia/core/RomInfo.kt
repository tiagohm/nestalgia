package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
data class RomInfo(
    val name: String,
    val format: RomFormat,
    val isNes20Header: Boolean = false,
    val isHeaderless: Boolean = false,
    val filePrgOffset: Int = 0,
    val mapperId: Int = 0,
    val subMapperId: Int = 0,
    val system: GameSystem = GameSystem.UNKNOWN,
    val vsType: VsSystemType = VsSystemType.DEFAULT,
    val inputType: GameInputType = GameInputType.UNSPECIFIED,
    val vsPpuModel: PpuModel = PpuModel.PPU_2C02,
    val hasChrRam: Boolean = false,
    val hasBattery: Boolean = false,
    val hasTreiner: Boolean = false,
    val mirroring: MirroringType = MirroringType.HORIZONTAL,
    val busConflict: BusConflictType = BusConflictType.DEFAULT,
    val hash: HashInfo = HashInfo.EMPTY,
    val header: NesHeader = NesHeader.EMPTY,
    val nsf: NsfHeader? = null,
    val unifBoard: String = "",
    val gameInfo: GameInfo? = null,
) {
    val isInDatabase = gameInfo != null
}