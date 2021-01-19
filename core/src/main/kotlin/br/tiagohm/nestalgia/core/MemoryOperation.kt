package br.tiagohm.nestalgia.core

enum class MemoryOperation {
    READ,
    WRITE,
    ANY;

    val isRead: Boolean
        get() = this == READ || this == ANY

    val isWrite: Boolean
        get() = this == WRITE || this == ANY
}
