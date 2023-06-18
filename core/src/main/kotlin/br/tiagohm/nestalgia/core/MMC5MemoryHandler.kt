package br.tiagohm.nestalgia.core

class MMC5MemoryHandler(private val console: Console) : MemoryHandler {

    private val ppuReg = IntArray(8)

    override fun memoryRanges(ranges: MemoryRanges) = Unit

    fun readRegister(addr: Int) = ppuReg[addr and 0x07]

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        console.ppu.write(addr, value, type)
        ppuReg[addr and 0x07] = value
    }
}
