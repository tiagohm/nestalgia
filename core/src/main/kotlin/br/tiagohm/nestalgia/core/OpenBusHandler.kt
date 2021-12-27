package br.tiagohm.nestalgia.core

class OpenBusHandler : MemoryHandler {

    var openBus: UByte = 0U

    override fun getMemoryRanges(ranges: MemoryRanges) {
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte = openBus

    override fun peek(addr: UShort) = addr.hiByte

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
    }
}

