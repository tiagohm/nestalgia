package br.tiagohm.nestalgia.core

abstract class ExpansionAudio(@JvmField protected val console: Console) : Snapshotable {

    abstract fun clockAudio()

    fun clock() {
        if (console.apu.enabled) {
            clockAudio()
        }
    }

    override fun saveState(s: Snapshot) {}

    override fun restoreState(s: Snapshot) {}
}
