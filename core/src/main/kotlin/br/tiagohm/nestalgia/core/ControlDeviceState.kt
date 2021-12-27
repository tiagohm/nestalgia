package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
data class ControlDeviceState(val state: UByteArray = UByteArray(8)) {

    inline fun clear() = state.fill(0U)
}