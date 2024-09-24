package br.tiagohm.nestalgia.core

import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
class ControlDeviceState(@PublishedApi @JvmField internal val state: IntArray) : Snapshotable {

    constructor(size: Int = 32) : this(IntArray(size))

    inline val size
        get() = state.size

    inline fun clear() {
        state.fill(0)
    }

    inline operator fun get(index: Int): Int {
        return state[index]
    }

    inline operator fun set(index: Int, value: Int) {
        state[index] = value
    }

    override fun saveState(s: Snapshot) {
        s.write("state", state)
    }

    override fun restoreState(s: Snapshot) {
        val state = s.readIntArray("state") ?: return clear()
        state.copyInto(this.state, 0, 0, min(state.size, size))
    }
}
