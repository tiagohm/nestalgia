package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

class InternalRamHandler(
    private val ram: IntArray,
    private val mask: Int,
) : MemoryHandler {

    override fun memoryRanges(ranges: MemoryRanges) {
        ranges.allowOverride = true
        ranges.addHandler(READ_WRITE, 0, 0x1FFF)
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return ram[addr and mask]
    }

    override fun peek(addr: Int): Int {
        return read(addr)
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        ram[addr and mask] = value
    }
}
