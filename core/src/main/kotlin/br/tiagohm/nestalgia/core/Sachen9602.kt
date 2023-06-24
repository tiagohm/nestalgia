package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_513

class Sachen9602(console: Console) : MMC3(console) {

    override val chrRamSize = 0x8000

    override val isForceChrBattery = true

    private val regs = IntArray(2)

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, (page and 0x3F) or (regs[1] shl 6), memoryType)
        super.selectPrgPage(if (prgMode != 0) 0 else 2, 0x3E, memoryType)
        super.selectPrgPage(3, 0x3F, memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        var newValue = value

        when (addr and 0xE001) {
            0x8000 -> regs[0] = value
            0x8001 -> if (regs[0] and 0x07 < 6) {
                regs[1] = value shr 6
                newValue = value and 0x1F
                updatePrgMapping()
            }
        }

        super.writeRegister(addr, newValue)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
    }
}
