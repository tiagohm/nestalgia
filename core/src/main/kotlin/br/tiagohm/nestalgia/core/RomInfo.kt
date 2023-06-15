package br.tiagohm.nestalgia.core

data class RomInfo(
    @JvmField val name: String = "",
    @JvmField val format: RomFormat = RomFormat.UNKNOWN,
    @JvmField val isNes20Header: Boolean = false,
    @JvmField val isHeaderless: Boolean = false,
    @JvmField val filePrgOffset: Int = 0,
    @JvmField val mapperId: Int = 0,
    @JvmField val subMapperId: Int = 0,
    @JvmField var system: GameSystem = GameSystem.UNKNOWN,
    @JvmField var vsType: VsSystemType = VsSystemType.DEFAULT,
    @JvmField val inputType: GameInputType = GameInputType.UNSPECIFIED,
    @JvmField val vsPpuModel: PpuModel = PpuModel.PPU_2C02,
    @JvmField val hasChrRam: Boolean = false,
    @JvmField val hasBattery: Boolean = false,
    @JvmField val hasTreiner: Boolean = false,
    @JvmField val mirroring: MirroringType = MirroringType.HORIZONTAL,
    @JvmField val busConflict: BusConflictType = BusConflictType.DEFAULT,
    @JvmField val hash: HashInfo = HashInfo.EMPTY,
    @JvmField val header: NesHeader = NesHeader.EMPTY,
    @JvmField val nsf: NsfHeader? = null,
    @JvmField val unifBoard: String = "",
    @JvmField val gameInfo: GameInfo? = null,
) {

    @JvmField val isInDatabase = gameInfo != null

    companion object {

        @JvmStatic val EMPTY = RomInfo()
    }
}
