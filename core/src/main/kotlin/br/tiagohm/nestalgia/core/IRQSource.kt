package br.tiagohm.nestalgia.core

enum class IRQSource(@PublishedApi @JvmField internal val code: Int) : Flag {
    EXTERNAL(1),
    FRAME_COUNTER(2),
    DMC(4),
    FDS_DISK(8),
}
