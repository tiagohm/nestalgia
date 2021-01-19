package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
data class RomInfo(
    val name: String,
    val format: RomFormat,
    val isNes20Header: Boolean,
    val isHeaderless: Boolean,
    val filePrgOffset: Int,
    val mapperId: Int,
    val subMapperId: Int,
    val system: GameSystem,
    val vsType: VsSystemType,
    val inputType: GameInputType,
    val vsPpuModel: PpuModel,
    val hasChrRam: Boolean,
    val hasBattery: Boolean,
    val hasTreiner: Boolean,
    val mirroring: MirroringType,
    val busConflict: BusConflictType,
    val hash: HashInfo,
    val header: NesHeader,
    val nsf: NsfHeader?,
    val gameInfo: GameInfo?,
) {
    val isInDatabase = gameInfo != null
}