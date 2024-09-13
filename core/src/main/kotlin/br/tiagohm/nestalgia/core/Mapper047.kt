package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_047

class Mapper047(console: Console) : MMC3(console) {

    @Volatile private var selectedBlock = 0

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, (page and 0x7F) or if (selectedBlock == 1) 0x80 else 0x00, memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, (page and 0x0F) or if (selectedBlock == 1) 0x10 else 0x00, memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (canWriteToWram) {
                selectedBlock = value and 0x01
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
