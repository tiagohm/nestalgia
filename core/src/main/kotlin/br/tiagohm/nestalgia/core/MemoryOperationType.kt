package br.tiagohm.nestalgia.core

enum class MemoryOperationType {
    READ,
    WRITE,
    OPCODE,
    OPERAND,
    DMA_READ,
    DMA_WRITE,
    DUMMY_READ,
    DUMMY_WRITE,
    PPU_RENDERING_READ,
}
