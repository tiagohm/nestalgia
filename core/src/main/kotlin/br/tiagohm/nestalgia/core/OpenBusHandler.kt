package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class OpenBusHandler : MemoryHandler {

    var openBus: UByte = 0U

    override fun getMemoryRanges(ranges: MemoryRanges) {
        // nada
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        return openBus
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        // nada
    }
}

