package br.tiagohm.nestalgia.core

abstract class ExpansionAudio(@JvmField protected val console: Console) : Clockable, Snapshotable {

    protected abstract fun clockAudio()

    override fun clock() {
        if (console.apu.enabled) {
            clockAudio()
        }
    }
}
