package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperationType.*

sealed interface Memory {

    fun read(addr: Int, type: MemoryOperationType = MEMORY_READ) = 0

    fun write(addr: Int, value: Int, type: MemoryOperationType = MEMORY_WRITE) = Unit

    fun readWord(addr: Int, type: MemoryOperationType = MEMORY_READ): Int {
        val lo = read(addr, type)
        val hi = read(addr + 1, type)
        return lo or (hi shl 8)
    }
}
