package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperationType.MEMORY_READ
import br.tiagohm.nestalgia.core.MemoryOperationType.MEMORY_WRITE

sealed interface Memory : Writable, Readable {

    fun read(addr: Int, type: MemoryOperationType) = 0

    fun write(addr: Int, value: Int, type: MemoryOperationType) = Unit

    override fun read(addr: Int): Int {
        return read(addr, MEMORY_READ)
    }

    override fun write(addr: Int, value: Int) {
        write(addr, value, MEMORY_WRITE)
    }

    fun readWord(addr: Int, type: MemoryOperationType = MEMORY_READ): Int {
        val lo = read(addr, type)
        val hi = read(addr + 1, type)
        return lo or (hi shl 8)
    }
}
