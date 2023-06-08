package br.tiagohm.nestalgia.core

@Suppress("ArrayInDataClass")
data class ControlDeviceState(@JvmField val state: IntArray = IntArray(8)) {

    fun clear() {
        state.fill(0)
    }
}
