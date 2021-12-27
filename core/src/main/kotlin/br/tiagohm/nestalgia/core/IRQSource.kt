package br.tiagohm.nestalgia.core

enum class IRQSource(override val code: UByte) : Flag<UByte> {
    EXTERNAL(1U),
    FRAME_COUNTER(2U),
    DMC(4U),
    FDS_DISK(8U),
}