@file:Suppress("NOTHING_TO_INLINE")

package br.tiagohm.nestalgia.core

// Boolean

inline fun Boolean.toInt() = if (this) 1 else 0

// UByte

@ExperimentalUnsignedTypes
inline val UByte.bit0: Boolean
    get() = (this.toUInt() and 0x01U) == 0x01U

@ExperimentalUnsignedTypes
inline val UByte.bit1: Boolean
    get() = (this.toUInt() and 0x02U) == 0x02U

@ExperimentalUnsignedTypes
inline val UByte.bit2: Boolean
    get() = (this.toUInt() and 0x04U) == 0x04U

@ExperimentalUnsignedTypes
inline val UByte.bit3: Boolean
    get() = (this.toUInt() and 0x08U) == 0x08U

@ExperimentalUnsignedTypes
inline val UByte.bit4: Boolean
    get() = (this.toUInt() and 0x10U) == 0x10U

@ExperimentalUnsignedTypes
inline val UByte.bit5: Boolean
    get() = (this.toUInt() and 0x20U) == 0x20U

@ExperimentalUnsignedTypes
inline val UByte.bit6: Boolean
    get() = (this.toUInt() and 0x40U) == 0x40U

@ExperimentalUnsignedTypes
inline val UByte.bit7: Boolean
    get() = (this.toUInt() and 0x080U) == 0x80U

@ExperimentalUnsignedTypes
inline val UByte.isZero: Boolean
    get() = this == 0.toUByte()

@ExperimentalUnsignedTypes
inline val UByte.isNonZero: Boolean
    get() = !isZero

@ExperimentalUnsignedTypes
inline val UByte.isFilled: Boolean
    get() = toUInt() and 0xFFU == 0xFFU

@ExperimentalUnsignedTypes
inline fun UByte.plusOne(): UByte {
    return (this + 1U).toUByte()
}

@ExperimentalUnsignedTypes
inline fun UByte.minusOne(): UByte {
    return (this - 1U).toUByte()
}

@ExperimentalUnsignedTypes
inline infix fun UByte.shr(n: Int): UByte {
    return (toUInt() shr n).toUByte()
}

// UShort

@ExperimentalUnsignedTypes
inline val UShort.bit0: Boolean
    get() = (toUInt() and 0x01U) == 0x01U

@ExperimentalUnsignedTypes
inline val UShort.bit1: Boolean
    get() = (toUInt() and 0x02U) == 0x02U

@ExperimentalUnsignedTypes
inline val UShort.bit2: Boolean
    get() = (toUInt() and 0x04U) == 0x04U

@ExperimentalUnsignedTypes
inline val UShort.bit3: Boolean
    get() = (toUInt() and 0x08U) == 0x08U

@ExperimentalUnsignedTypes
inline val UShort.bit4: Boolean
    get() = (toUInt() and 0x10U) == 0x10U

@ExperimentalUnsignedTypes
inline val UShort.bit5: Boolean
    get() = (toUInt() and 0x20U) == 0x20U

@ExperimentalUnsignedTypes
inline val UShort.bit6: Boolean
    get() = (toUInt() and 0x40U) == 0x40U

@ExperimentalUnsignedTypes
inline val UShort.bit7: Boolean
    get() = (toUInt() and 0x080U) == 0x80U

@ExperimentalUnsignedTypes
inline val UShort.loByte: UByte
    get() = toUByte()

@ExperimentalUnsignedTypes
inline val UShort.hiByte: UByte
    get() = (toUInt() shr 8).toUByte()

@ExperimentalUnsignedTypes
inline fun makeUShort(lo: UByte, hi: UByte): UShort {
    return (lo.toUInt() or (hi.toUInt() shl 8)).toUShort()
}

@ExperimentalUnsignedTypes
inline fun UShort.plusOne(): UShort {
    return (this + 1U).toUShort()
}

@ExperimentalUnsignedTypes
inline fun UShort.minusOne(): UShort {
    return (this - 1U).toUShort()
}

@ExperimentalUnsignedTypes
inline infix fun UShort.shr(n: Int): UShort {
    return (toUInt() shr n).toUShort()
}

@ExperimentalUnsignedTypes
inline val UShort.isZero: Boolean
    get() = this == 0.toUShort()

// UInt

@ExperimentalUnsignedTypes
inline val UInt.bit0: Boolean
    get() = (this and 0x01U) == 0x01U

@ExperimentalUnsignedTypes
inline val UInt.loByte: UByte
    get() = toUByte()

@ExperimentalUnsignedTypes
inline val UInt.hiByte: UByte
    get() = (this shr 8).toUByte()

@ExperimentalUnsignedTypes
inline val UInt.higherByte: UByte
    get() = (this shr 16).toUByte()

@ExperimentalUnsignedTypes
inline val UInt.highestByte: UByte
    get() = (this shr 24).toUByte()

@ExperimentalUnsignedTypes
inline fun makeUInt(lo: UByte, hi: UByte, higher: UByte, highest: UByte): UInt {
    return lo.toUInt() or (hi.toUInt() shl 8) or (higher.toUInt() shl 16) or (highest.toUInt() shl 24)
}

@ExperimentalUnsignedTypes
inline fun makeUInt(lo: UShort, hi: UShort): UInt {
    return lo.toUInt() or (hi.toUInt() shl 16)
}

// Int

inline fun makeInt(lo: Byte, hi: Byte, higher: Byte, highest: Byte): Int {
    return (lo.toInt() and 0xFF) or (hi.toInt() and 0xFF shl 8) or (higher.toInt() and 0xFF shl 16) or (highest.toInt() and 0xFF shl 24)
}

// ULong

@ExperimentalUnsignedTypes
inline val ULong.bit0: Boolean
    get() = (this and 0x01UL) == 0x01UL
