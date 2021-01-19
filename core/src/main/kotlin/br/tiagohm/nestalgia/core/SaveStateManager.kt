package br.tiagohm.nestalgia.core

import java.io.IOException

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class SaveStateManager(val console: Console) : Resetable {

    override fun reset(softReset: Boolean) {
    }

    @Synchronized
    fun saveState(): ByteArray {
        return if (console.isRunning) {
            try {
                console.pause()
                return Snapshot().also { saveState(it) }.bytes
            } finally {
                console.resume()
            }
        } else {
            ByteArray(0)
        }
    }

    private fun saveState(s: Snapshot) {
        val data = Snapshot()

        console.saveState(data)

        val state = SaveStateData(
            VERSION,
            console.mapper!!.info.mapperId,
            console.mapper!!.info.subMapperId,
            console.mapper!!.info.hash.prgChrCrc32,
            data,
        )

        state.saveState(s)
    }

    // Restore

    @Synchronized
    fun restoreState(data: ByteArray) {
        if (console.isRunning) {
            try {
                console.pause()
                restoreState(Snapshot(data))
            } finally {
                console.resume()
            }
        }
    }

    private fun restoreState(s: Snapshot) {
        val state = SaveStateData()

        state.restoreState(s)

        if (state.version != VERSION) {
            throw IOException("Incompatible version: ${state.version}")
        }

        if (console.mapper!!.info.mapperId != state.mapperId) {
            throw IOException("Mismatch mapper ID: ${state.mapperId}")
        }

        if (console.mapper!!.info.hash.prgChrCrc32 != state.hash) {
            throw IOException(String.format("Mismatch ROM hash: %08X", state.hash))
        }

        console.restoreState(state.data)
    }

    companion object {
        const val VERSION = 1
    }
}
