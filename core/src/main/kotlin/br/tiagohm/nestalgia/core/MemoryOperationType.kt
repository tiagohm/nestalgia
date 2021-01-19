package br.tiagohm.nestalgia.core

enum class MemoryOperationType {
    READ,
    WRITE,
    OPCODE,
    OPERAND,
    PPU_RENDERING_READ,
    DUMMY_READ,
    DMC_READ,
    DUMMY_WRITE,
}