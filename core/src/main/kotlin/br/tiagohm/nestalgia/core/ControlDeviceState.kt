package br.tiagohm.nestalgia.core

import kotlin.math.min

data class ControlDeviceState(private val state: IntArray) : Snapshotable {

    constructor(size: Int = 32) : this(IntArray(size))

    val size: Int
        get() = state.size

    fun clear() {
        state.fill(0)
    }

    operator fun get(index: Int): Int {
        return state[index]
    }

    operator fun set(index: Int, value: Int) {
        state[index] = value
    }

    override fun saveState(s: Snapshot) {
        s.write("state", state)
    }

    override fun restoreState(s: Snapshot) {
        val state = s.readIntArray("state") ?: return clear()
        state.copyInto(this.state, 0, 0, min(state.size, size))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ControlDeviceState

        return state.contentEquals(other.state)
    }

    override fun hashCode() = state.contentHashCode()
}
