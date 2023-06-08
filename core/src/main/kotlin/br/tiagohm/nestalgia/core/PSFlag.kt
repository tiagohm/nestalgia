package br.tiagohm.nestalgia.core

enum class PSFlag(@JvmField internal val code: Int) : Flag {
    CARRY(0x01),
    ZERO(0x02),
    INTERRUPT(0x04),
    DECIMAL(0x08),
    BREAK(0x10),
    RESERVED(0x20),
    OVERFLOW(0x40),
    NEGATIVE(0x80),
}
