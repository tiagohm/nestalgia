package br.tiagohm.nestalgia.core

data class PpuControl(
    var verticalWrite: Boolean = false,
    var spritePatternAddr: Int = 0,
    var backgroundPatternAddr: Int = 0,
    var largeSprites: Boolean = false,
    var vBlank: Boolean = false,
    var grayscale: Boolean = false,
    var backgroundMask: Boolean = false,
    var spriteMask: Boolean = false,
    var backgroundEnabled: Boolean = false,
    var spritesEnabled: Boolean = false,
    var intensifyRed: Boolean = false,
    var intensifyGreen: Boolean = false,
    var intensifyBlue: Boolean = false,
) : Snapshotable {

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
