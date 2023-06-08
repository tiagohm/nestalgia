package br.tiagohm.nestalgia.core

enum class MemoryAccessType(val read: Boolean, val write: Boolean) {
    UNSPECIFIED(true, true),
    NO_ACCESS(false, false),
    READ(true, false),
    WRITE(false, true),
    READ_WRITE(true, true),
}
