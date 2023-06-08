package br.tiagohm.nestalgia.core

data class SaveStateData(
    var mapperId: Int = -1,
    var subMapperId: Int = -1,
    var hash: String = "",
    var data: Snapshot = Snapshot(),
) : Snapshotable {

    override fun saveState(s: Snapshot) {
        s.write("mapperId", mapperId)
        s.write("subMapperId", subMapperId)
        s.write("hash", hash)
        s.write("data", data)
    }

    override fun restoreState(s: Snapshot) {
        mapperId = s.readInt("mapperId", -1)
        subMapperId = s.readInt("subMapperId", -1)
        hash = s.readString("hash")
        s.readSnapshot("data")?.let { data = it }
    }
}
