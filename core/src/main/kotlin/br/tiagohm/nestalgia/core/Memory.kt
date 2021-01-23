package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
interface Memory {

    fun read(addr: UShort, type: MemoryOperationType = MemoryOperationType.READ): UByte

    fun write(addr: UShort, value: UByte, type: MemoryOperationType = MemoryOperationType.WRITE)

    fun readWord(addr: UShort, type: MemoryOperationType = MemoryOperationType.READ): UShort {
        val lo = read(addr, type)
        val hi = read(addr.plusOne(), type)
        return makeUShort(lo, hi)
    }
}