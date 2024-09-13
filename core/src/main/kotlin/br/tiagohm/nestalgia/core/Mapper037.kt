package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_037

class Mapper037(console: Console) : MMC3(console) {

    @Volatile private var selectedBlock = 0

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        selectedBlock = 0
        updateState()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, if (selectedBlock >= 4) (page or 0x80) else page, memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        when {
            selectedBlock <= 2 -> super.selectPrgPage(slot, page and 0x07, memoryType)
            selectedBlock == 3 -> super.selectPrgPage(slot, (page and 0x07) or 0x08, memoryType)
            selectedBlock == 7 -> super.selectPrgPage(slot, (page and 0x07) or 0x20, memoryType)
            else -> super.selectPrgPage(slot, (page and 0x0F) or 0x10, memoryType)
            // else -> super.selectPrgPage(slot, page, memoryType)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (canWriteToWram) {
                selectedBlock = value and 0x07
                updateState()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("selectedBlock", selectedBlock)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        selectedBlock = s.readInt("selectedBlock")
    }
}
