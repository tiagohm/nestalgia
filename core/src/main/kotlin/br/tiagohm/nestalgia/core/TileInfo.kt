package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
data class TileInfo(
    var lowByte: UByte = 0U,
    var highByte: UByte = 0U,
    var paletteOffset: UInt = 0U,
    var tileAddr: UShort = 0U,
    var offsetY: UByte = 0U,
) : Snapshotable {

    inline fun copyFrom(tile: TileInfo) {
        lowByte = tile.lowByte
        highByte = tile.highByte
        paletteOffset = tile.paletteOffset
        tileAddr = tile.tileAddr
        offsetY = tile.offsetY
    }

    override fun saveState(s: Snapshot) {
        s.write("lowByte", lowByte)
        s.write("highByte", highByte)
        s.write("paletteOffset", paletteOffset)
        s.write("tileAddr", tileAddr)
        s.write("offsetY", offsetY)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        lowByte = s.readUByte("lowByte") ?: 0U
        highByte = s.readUByte("highByte") ?: 0U
        paletteOffset = s.readUInt("paletteOffset") ?: 0U
        tileAddr = s.readUShort("tileAddr") ?: 0U
        offsetY = s.readUByte("offsetY") ?: 0U
    }
}