package br.tiagohm.nestalgia.core

enum class MemoryOperationType {
    MEMORY_READ,
    MEMORY_WRITE,
    OPCODE,
    OPERAND,
    DMA_READ,
    DMA_WRITE,
    DUMMY_READ,
    DUMMY_WRITE,
    PPU_RENDERING_READ,
}
