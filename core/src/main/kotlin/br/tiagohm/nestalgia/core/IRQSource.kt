package br.tiagohm.nestalgia.core

enum class IRQSource(@PublishedApi @JvmField internal val code: Int) : Flag {
    EXTERNAL(0x01),
    FRAME_COUNTER(0x02),
    DMC(0x04),
    FDS_DISK(0x08),
}
