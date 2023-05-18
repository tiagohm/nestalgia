package br.tiagohm.nestalgia.core

data class SaveStateData(
    var version: Int = 0,
    var mapperId: Int = -1,
    var subMapperId: Int = -1,
    var hash: Long = 0L,
    var data: Snapshot = Snapshot(),
) : Snapshotable {

    override fun saveState(s: Snapshot) {
        s.write("version", version)
        s.write("mapperId", mapperId)
        s.write("subMapperId", subMapperId)
        s.write("hash", hash)
        s.write("data", data)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        version = s.readInt("version") ?: 0
        mapperId = s.readInt("mapperId") ?: -1
        subMapperId = s.readInt("subMapperId") ?: -1
        hash = s.readLong("hash") ?: 0L
        s.readSnapshot("data")?.let { data = it }
    }
}
