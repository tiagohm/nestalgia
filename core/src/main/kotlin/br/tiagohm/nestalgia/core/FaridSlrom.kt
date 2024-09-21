package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_323

class FaridSlrom(console: Console) : MMC1(console) {

    @Volatile private var outerBank = 0
    @Volatile private var locked = false

    override fun initialize() {
        addRegisterRange(0x6000, 0x7FFF, MemoryAccessType.WRITE)
        super.initialize()
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        outerBank = 0
        locked = false
        updateState()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, (outerBank shl 2) or (page and 0x1F), memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, outerBank or (page and 0x07), memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            val wramEnabled = !wramDisable

            if (wramEnabled && !locked) {
                outerBank = value and 0x70 shr 1
                locked = value.bit3
                updateState()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }
}
