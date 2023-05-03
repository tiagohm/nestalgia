package br.tiagohm.nestalgia.core

data class PpuControl(
    var verticalWrite: Boolean = false,
    var spritePatternAddr: UShort = 0U,
    var backgroundPatternAddr: UShort = 0U,
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
        s.load()

        verticalWrite = s.readBoolean("verticalWrite") ?: false
        spritePatternAddr = s.readUShort("spritePatternAddr") ?: 0U
        backgroundPatternAddr = s.readUShort("backgroundPatternAddr") ?: 0U
        largeSprites = s.readBoolean("largeSprites") ?: false
        vBlank = s.readBoolean("vBlank") ?: false
        grayscale = s.readBoolean("grayscale") ?: false
        backgroundMask = s.readBoolean("backgroundMask") ?: false
        spriteMask = s.readBoolean("spriteMask") ?: false
        backgroundEnabled = s.readBoolean("backgroundEnabled") ?: false
        spritesEnabled = s.readBoolean("spritesEnabled") ?: false
        intensifyRed = s.readBoolean("intensifyRed") ?: false
        intensifyGreen = s.readBoolean("intensifyGreen") ?: false
        intensifyBlue = s.readBoolean("intensifyBlue") ?: false
    }
}