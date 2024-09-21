package br.tiagohm.nestalgia.core

class OpenBusHandler : MemoryHandler {

    @JvmField @Volatile var openBus = 0

    override fun memoryRanges(ranges: MemoryRanges) = Unit

    override fun read(addr: Int, type: MemoryOperationType) = openBus

    override fun peek(addr: Int) = addr.hiByte
}
