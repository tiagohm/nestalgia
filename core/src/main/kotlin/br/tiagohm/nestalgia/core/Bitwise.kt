@file:Suppress("NOTHING_TO_INLINE")

package br.tiagohm.nestalgia.core

// Boolean.

inline fun Boolean.toInt() = if (this) 1 else 0

// Int.

inline val Int.bit0
    get() = (this and 0x01) == 0x01

inline val Int.bit1
    get() = (this and 0x02) == 0x02

inline val Int.bit2
    get() = (this and 0x04) == 0x04

inline val Int.bit3
    get() = (this and 0x08) == 0x08

inline val Int.bit4
    get() = (this and 0x10) == 0x10

inline val Int.bit5
    get() = (this and 0x20) == 0x20

inline val Int.bit6
    get() = (this and 0x40) == 0x40

inline val Int.bit7
    get() = (this and 0x080) == 0x80

inline val Int.loByte
    get() = this and 0xFF

inline val Int.hiByte
    get() = (this shr 8) and 0xFF

inline val Int.higherByte
    get() = (this shr 16) and 0xFF

inline val Int.highestByte
    get() = (this shr 24) and 0xFF

// Long.

inline val Long.bit0
    get() = (this and 0x01L) == 0x01L
