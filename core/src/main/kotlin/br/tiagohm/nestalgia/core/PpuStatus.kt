package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
data class PpuStatus(
    var spriteOverflow: Boolean = false,
    var sprite0Hit: Boolean = false,
    var verticalBlank: Boolean = false,
) : Snapshotable {

    override fun saveState(s: Snapshot) {
        s.write("spriteOverflow", spriteOverflow)
        s.write("sprite0Hit", sprite0Hit)
        s.write("verticalBlank", verticalBlank)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        spriteOverflow = s.readBoolean("spriteOverflow") ?: false
        sprite0Hit = s.readBoolean("sprite0Hit") ?: false
        verticalBlank = s.readBoolean("verticalBlank") ?: false
    }
}