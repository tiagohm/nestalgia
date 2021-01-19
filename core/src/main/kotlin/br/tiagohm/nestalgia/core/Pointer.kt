package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
open class Pointer protected constructor(
    val type: MemoryType?,
    val data: UByteArray,
    val offset: Int = 0,
) {

    constructor(pointer: Pointer, offset: Int = 0) : this(pointer.type, pointer.data, pointer.offset + offset)

    val size = data.size

    val isEmpty = data.isEmpty()

    inline operator fun get(index: Int): UByte = data[offset + index]

    inline operator fun set(index: Int, value: UByte) {
        data[offset + index] = value
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
        val NULL = Pointer(null, UByteArray(0), 0)

        fun rom(data: UByteArray, offset: Int = 0) = Pointer(PrgMemoryType.ROM, data, offset)

        fun sram(data: UByteArray, offset: Int = 0) = Pointer(PrgMemoryType.SRAM, data, offset)

        fun wram(data: UByteArray, offset: Int = 0) = Pointer(PrgMemoryType.WRAM, data, offset)

        fun nametable(data: UByteArray, offset: Int = 0) = Pointer(ChrMemoryType.NAMETABLE_RAM, data, offset)

        fun chrRam(data: UByteArray, offset: Int = 0) = Pointer(ChrMemoryType.RAM, data, offset)

        fun chrRom(data: UByteArray, offset: Int = 0) = Pointer(ChrMemoryType.ROM, data, offset)
    }
}