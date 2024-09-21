package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_366

class BmcGn45(console: Console) : MMC3(console) {

    @Volatile private var selectedBlock = 0
    @Volatile private var wramEnabled = false

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        if (softReset) {
            selectedBlock = 0
            wramEnabled = false
            resetMMC3()
            updateState()
        }
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, (page and 0x7F) or (selectedBlock shl 3), memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, (page and 0x0F) or selectedBlock, memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x7000) {
            if (!wramEnabled) {
                selectedBlock = addr and 0x30
                wramEnabled = addr.bit7
                updateState()
            } else {
                writePrgRam(addr, value)
            }
        } else if (addr < 0x8000) {
            if (!wramEnabled) {
                selectedBlock = value and 0x30
                updateState()
            } else {
                writePrgRam(addr, value)
            }
        } else {
            super.writeRegister(addr, value)
        }
    }
}
