package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
data class ControlDeviceState(val state: UByteArray = UByteArray(8))