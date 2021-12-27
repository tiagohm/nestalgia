package br.tiagohm.nestalgia.core

class InternalRamHandler(
    private val ram: UByteArray,
    private val mask: UShort
) : MemoryHandler {

    override fun getMemoryRanges(ranges: MemoryRanges) {
        ranges.allowOverride = true
        ranges.addHandler(MemoryOperation.ANY, 0U, 0x1FFFU)
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        return ram[(addr and mask).toInt()]
    }

    override fun peek(addr: UShort) = read(addr)

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        ram[(addr and mask).toInt()] = value
    }
}