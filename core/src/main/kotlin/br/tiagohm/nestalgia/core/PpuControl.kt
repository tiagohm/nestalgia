package br.tiagohm.nestalgia.core

data class PpuControl(
    @JvmField var verticalWrite: Boolean = false,
    @JvmField var spritePatternAddr: Int = 0,
    @JvmField var backgroundPatternAddr: Int = 0,
    @JvmField var largeSprites: Boolean = false,
    @JvmField var vBlank: Boolean = false,
    @JvmField var grayscale: Boolean = false,
    @JvmField var backgroundMask: Boolean = false,
    @JvmField var spriteMask: Boolean = false,
    @JvmField var backgroundEnabled: Boolean = false,
    @JvmField var spritesEnabled: Boolean = false,
    @JvmField var intensifyRed: Boolean = false,
    @JvmField var intensifyGreen: Boolean = false,
    @JvmField var intensifyBlue: Boolean = false,
) : Snapshotable, Resetable {

    override fun reset(softReset: Boolean) {
        verticalWrite = false
        spritePatternAddr = 0
        backgroundPatternAddr = 0
        largeSprites = false
        vBlank = false
        grayscale = false
        backgroundMask = false
        spriteMask = false
        backgroundEnabled = false
        spritesEnabled = false
        intensifyRed = false
        intensifyGreen = false
        intensifyBlue = false
    }

    override fun saveState(s: Snapshot) {
        s.write("verticalWrite", verticalWrite)
        s.write("spritePatternAddr", spritePatternAddr)
        s.write("backgroundPatternAddr", backgroundPatternAddr)
        s.write("largeSprites", largeSprites)
        s.write("vBlank", vBlank)
        s.write("grayscale", grayscale)
        s.write("backgroundMask", backgroundMask)
        s.write("spriteMask", spriteMask)
        s.write("backgroundEnabled", backgroundEnabled)
        s.write("spritesEnabled", spritesEnabled)
        s.write("intensifyRed", intensifyRed)
        s.write("intensifyGreen", intensifyGreen)
        s.write("intensifyBlue", intensifyBlue)
    }

    override fun restoreState(s: Snapshot) {
        verticalWrite = s.readBoolean("verticalWrite")
        spritePatternAddr = s.readInt("spritePatternAddr")
        backgroundPatternAddr = s.readInt("backgroundPatternAddr")
        largeSprites = s.readBoolean("largeSprites")
        vBlank = s.readBoolean("vBlank")
        grayscale = s.readBoolean("grayscale")
        backgroundMask = s.readBoolean("backgroundMask")
        spriteMask = s.readBoolean("spriteMask")
        backgroundEnabled = s.readBoolean("backgroundEnabled")
        spritesEnabled = s.readBoolean("spritesEnabled")
        intensifyRed = s.readBoolean("intensifyRed")
        intensifyGreen = s.readBoolean("intensifyGreen")
        intensifyBlue = s.readBoolean("intensifyBlue")
    }
}
