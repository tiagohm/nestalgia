package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
data class TileInfo(
    @JvmField var lowByte: Int = 0,
    @JvmField var highByte: Int = 0,
    @JvmField var paletteOffset: Int = 0,
    @JvmField var tileAddr: Int = 0,
    @JvmField var offsetY: Int = 0,
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
        lowByte = s.readInt("lowByte")
        highByte = s.readInt("highByte")
        paletteOffset = s.readInt("paletteOffset")
        tileAddr = s.readInt("tileAddr")
        offsetY = s.readInt("offsetY")
    }
}
