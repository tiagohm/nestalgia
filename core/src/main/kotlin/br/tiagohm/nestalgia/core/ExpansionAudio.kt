package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
abstract class ExpansionAudio(val console: Console) : Snapshotable {

    abstract fun clockAudio()

    inline fun clock() {
        if (console.apu.isEnabled) {
            clockAudio()
        }
    }

    override fun saveState(s: Snapshot) {
    }

    override fun restoreState(s: Snapshot) {
        s.load()
    }
}