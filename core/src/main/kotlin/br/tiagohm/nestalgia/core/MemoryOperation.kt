package br.tiagohm.nestalgia.core

enum class MemoryOperation(val read: Boolean, val write: Boolean) {
    READ(true, false),
    WRITE(false, true),
    ANY(true, true),
}
