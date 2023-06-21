package br.tiagohm.nestalgia.core

enum class PpuRegister(override val address: Int) : Register {
    CONTROL(0x00),
    MASK(0x01),
    STATUS(0x02),
    SPRITE_ADDR(0x03),
    SPRITE_DATA(0x04),
    SCROLL_OFFSET(0x05),
    VIDEO_MEMORY_ADDR(0x06),
    VIDEO_MEMORY_DATA(0x07),
    SPRITE_DMA(0x4014),
}
