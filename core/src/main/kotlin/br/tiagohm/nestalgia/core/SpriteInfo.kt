package br.tiagohm.nestalgia.core

data class SpriteInfo(
    @JvmField var lowByte: Int = 0,
    @JvmField var highByte: Int = 0,
    @JvmField var paletteOffset: Int = 0,
    @JvmField var tileAddr: Int = 0,
    @JvmField var horizontalMirror: Boolean = false,
    @JvmField var backgroundPriority: Boolean = false,
    @JvmField var spriteX: Int = 0,
    @JvmField var verticalMirror: Boolean = false, // HD PPU
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
        lowByte = s.readInt("lowByte")
        highByte = s.readInt("highByte")
        paletteOffset = s.readInt("paletteOffset")
        tileAddr = s.readInt("tileAddr")
        horizontalMirror = s.readBoolean("horizontalMirror")
        backgroundPriority = s.readBoolean("backgroundPriority")
        spriteX = s.readInt("spriteX")
        verticalMirror = s.readBoolean("verticalMirror")
    }
}
