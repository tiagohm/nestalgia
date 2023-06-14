package br.tiagohm.nestalgia.core

interface Snapshotable {

    fun saveState(s: Snapshot)

    fun restoreState(s: Snapshot)

    fun copyTo(snapshotable: Snapshotable) {
        val snapshot = Snapshot()
        saveState(snapshot)
        snapshotable.restoreState(snapshot)
    }
}
