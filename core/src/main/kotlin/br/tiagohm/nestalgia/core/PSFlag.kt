package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
enum class PSFlag(override val code: UByte) : Flag<UByte> {
    CARRY(0x01U),
    ZERO(0x02U),
    INTERRUPT(0x04U),
    DECIMAL(0x08U),
    BREAK(0x10U),
    RESERVED(0x20U),
    OVERFLOW(0x40U),
    NEGATIVE(0x80U)
}