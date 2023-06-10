package br.tiagohm.nestalgia.core

data class PpuStatus(
    var spriteOverflow: Boolean = false,
    var sprite0Hit: Boolean = false,
    var verticalBlank: Boolean = false,
) : Snapshotable, Resetable {

    override fun reset(softReset: Boolean) {
        spriteOverflow = false
        sprite0Hit = false
        verticalBlank = false
    }

    override fun saveState(s: Snapshot) {
        s.write("spriteOverflow", spriteOverflow)
        s.write("sprite0Hit", sprite0Hit)
        s.write("verticalBlank", verticalBlank)
    }

    override fun restoreState(s: Snapshot) {
        spriteOverflow = s.readBoolean("spriteOverflow")
        sprite0Hit = s.readBoolean("sprite0Hit")
        verticalBlank = s.readBoolean("verticalBlank")
    }
}
