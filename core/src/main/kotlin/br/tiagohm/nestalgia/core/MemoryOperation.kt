package br.tiagohm.nestalgia.core

enum class MemoryOperation(val isRead: Boolean, val isWrite: Boolean) {
    READ(true, false),
    WRITE(false, true),
    ANY(true, true),
}
