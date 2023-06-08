package br.tiagohm.nestalgia.core

import java.io.IOException

class SaveStateManager(private val console: Console) : Snapshotable, Resetable {

    override fun reset(softReset: Boolean) {}

    override fun saveState(s: Snapshot) {
        val data = Snapshot()

        console.saveState(data)

        val state = SaveStateData(
            console.mapper!!.info.mapperId,
            console.mapper!!.info.subMapperId,
            console.mapper!!.info.hash.sha256,
            data,
        )

        state.saveState(s)
    }

    override fun restoreState(s: Snapshot) {
        val state = SaveStateData()

        state.restoreState(s)

        if (console.mapper!!.info.mapperId != state.mapperId) {
            throw IOException("Mismatch mapper ID: ${state.mapperId}")
        }

        if (console.mapper!!.info.hash.sha256 != state.hash) {
            throw IOException(String.format("Mismatch ROM SHA-256 hash: %s", state.hash))
        }

        console.restoreState(state.data)
    }
}
