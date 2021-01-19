package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
interface Snapshotable {
    fun saveState(s: Snapshot)

    fun restoreState(s: Snapshot)
}