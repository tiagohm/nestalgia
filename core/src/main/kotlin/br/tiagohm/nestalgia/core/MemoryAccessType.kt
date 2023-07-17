package br.tiagohm.nestalgia.core

enum class MemoryAccessType(
    @JvmField internal val read: Boolean,
    @JvmField internal val write: Boolean,
) {
    UNSPECIFIED(true, true),
    NO_ACCESS(false, false),
    READ(true, false),
    WRITE(false, true),
    READ_WRITE(true, true),
}
