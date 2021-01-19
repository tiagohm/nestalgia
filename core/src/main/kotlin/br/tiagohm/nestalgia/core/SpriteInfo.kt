package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
data class SpriteInfo(
    var lowByte: UByte = 0U,
    var highByte: UByte = 0U,
    var paletteOffset: UInt = 0U,
    var tileAddr: UShort = 0U,
    var horizontalMirror: Boolean = false,
    var backgroundPriority: Boolean = false,
    var spriteX: UByte = 0U,
    var verticalMirror: Boolean = false, // HD PPU
) : Snapshotable {

    override fun saveState(s: Snapshot) {
        s.write("lowByte", lowByte)
        s.write("highByte", highByte)
        s.write("paletteOffset", paletteOffset)
        s.write("tileAddr", tileAddr)
        s.write("horizontalMirror", horizontalMirror)
        s.write("backgroundPriority", backgroundPriority)
        s.write("spriteX", spriteX)
        s.write("verticalMirror", verticalMirror)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        lowByte = s.readUByte("lowByte") ?: 0U
        highByte = s.readUByte("highByte") ?: 0U
        paletteOffset = s.readUInt("paletteOffset") ?: 0U
        tileAddr = s.readUShort("tileAddr") ?: 0U
        horizontalMirror = s.readBoolean("horizontalMirror") ?: false
        backgroundPriority = s.readBoolean("backgroundPriority") ?: false
        spriteX = s.readUByte("spriteX") ?: 0U
        verticalMirror = s.readBoolean("verticalMirror") ?: false
    }
}