package br.tiagohm.nestalgia.core

enum class PpuRegister(override val address: UShort) : Register {
    CONTROL(0x00U),
    MASK(0x01U),
    STATUS(0x02U),
    SPRITE_ADDR(0x03U),
    SPRITE_DATA(0x04U),
    SCROLL_OFFSET(0x05U),
    VIDEO_MEMORY_ADDR(0x06U),
    VIDEO_MEMORY_DATA(0x07U),
    SPRITE_DMA(0x4014U),
}