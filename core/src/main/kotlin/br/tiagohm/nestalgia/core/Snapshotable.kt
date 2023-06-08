package br.tiagohm.nestalgia.core

interface Snapshotable {

    fun saveState(s: Snapshot)

    fun restoreState(s: Snapshot)
}
