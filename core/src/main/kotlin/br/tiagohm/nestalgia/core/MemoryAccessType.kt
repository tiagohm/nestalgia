package br.tiagohm.nestalgia.core

enum class MemoryAccessType(val isRead: Boolean, val isWrite: Boolean) {
    UNSPECIFIED(true, true),
    NO_ACCESS(false, false),
    READ(true, false),
    WRITE(false, true),
    READ_WRITE(true, true),
}