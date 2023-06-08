package br.tiagohm.nestalgia.core

class OpenBusHandler : MemoryHandler {

    @JvmField var openBus = 0

    override fun memoryRanges(ranges: MemoryRanges) {}

    override fun read(addr: Int, type: MemoryOperationType) = openBus

    override fun peek(addr: Int) = addr.hiByte
}
