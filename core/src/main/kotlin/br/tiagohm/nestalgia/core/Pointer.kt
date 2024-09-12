package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
open class Pointer(
    @PublishedApi @JvmField internal val data: IntArray,
    @PublishedApi @JvmField internal val offset: Int = 0,
) : Memory {

    constructor(pointer: Pointer, offset: Int = 0) : this(pointer.data, pointer.offset + offset)

    init {
        require(offset >= 0) { "invalid offset: $offset" }
    }

    inline val size
        get() = data.size

    operator fun contains(index: Int): Boolean {
        return offset + index >= 0 && offset + index < data.size
    }

    inline operator fun get(index: Int): Int {
        return data[offset + index]
    }

    inline operator fun set(index: Int, value: Int) {
        data[offset + index] = value
    }

    fun slice(range: IntRange): IntArray {
        // TODO: Remove sliceArray de todo o código.
        return data.sliceArray(offset + range.first..offset + range.last)
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        this[addr] = value
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return this[addr]
    }

    fun fill(value: Int, length: Int, startIndex: Int = 0) {
        data.fill(value, offset + startIndex, offset + startIndex + length)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pointer

        println("NUNCA ENTRAR AQUI!!!")

        return data.contentEquals(other.data)
    }

    override fun hashCode() = data.contentHashCode()

    companion object {

        internal val NULL = Pointer(IntArray(0), 0)
    }
}
