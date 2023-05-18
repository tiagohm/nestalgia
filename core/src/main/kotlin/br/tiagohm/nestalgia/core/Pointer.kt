package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
open class Pointer(
    val data: UByteArray,
    val offset: Int = 0,
) : Memory {

    constructor(pointer: Pointer, offset: Int = 0) : this(pointer.data, pointer.offset + offset)

    val size = data.size

    val isEmpty = data.isEmpty()

    val isNotEmpty = !isEmpty

    inline operator fun contains(index: Int) = offset + index >= 0 && offset + index < data.size

    inline operator fun get(index: Int): UByte = data[offset + index]

    inline operator fun set(index: Int, value: UByte) {
        data[offset + index] = value
    }

    inline fun slice(range: IntRange) = data.sliceArray(offset + range.first..offset + range.last)

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        this[addr.toInt()] = value
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        return this[addr.toInt()]
    }

    inline fun fill(value: UByte, length: Int, startIndex: Int = 0) {
        data.fill(value, offset + startIndex, offset + startIndex + length)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pointer

        if (data != other.data) return false
        if (offset != other.offset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + offset
        return result
    }

    companion object {

        @JvmStatic val NULL = Pointer(UByteArray(0), 0)
    }
}
