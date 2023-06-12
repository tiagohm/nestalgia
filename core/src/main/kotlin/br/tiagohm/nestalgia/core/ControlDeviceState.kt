package br.tiagohm.nestalgia.core

@Suppress("ArrayInDataClass", "NOTHING_TO_INLINE")
data class ControlDeviceState(@JvmField val state: IntArray = IntArray(8)) {

    inline fun clear() {
        state.fill(0)
    }
}
