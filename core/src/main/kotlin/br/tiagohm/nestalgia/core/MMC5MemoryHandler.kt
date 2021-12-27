package br.tiagohm.nestalgia.core

class MMC5MemoryHandler(val console: Console) : MemoryHandler {

    private val ppuReg = UByteArray(8)

    override fun getMemoryRanges(ranges: MemoryRanges) {
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte = 0U

    fun readRegister(addr: UShort) = ppuReg[addr.toInt() and 0x07]

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        console.ppu.write(addr, value, type)
        ppuReg[addr.toInt() and 0x07] = value
    }
}