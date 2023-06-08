package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
abstract class ExpansionAudio(@PublishedApi @JvmField internal val console: Console) : Snapshotable {

    abstract fun clockAudio()

    inline fun clock() {
        if (console.apu.enabled) {
            clockAudio()
        }
    }

    override fun saveState(s: Snapshot) {}

    override fun restoreState(s: Snapshot) {}
}
