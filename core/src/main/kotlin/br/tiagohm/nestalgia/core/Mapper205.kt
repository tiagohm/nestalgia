package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_205

class Mapper205(console: Console) : MMC3(console) {

    @Volatile private var selectedBlock = 0

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        var newPage = page

        if (selectedBlock >= 2) {
            newPage = newPage and 0x7F or 0x100
        }

        if (selectedBlock == 1 || selectedBlock == 3) {
            newPage = newPage or 0x80
        }

        super.selectChrPage(slot, newPage, memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        var newPage = page
        newPage = newPage and if (selectedBlock <= 1) 0x1F else 0x0F
        newPage = newPage or (selectedBlock * 0x10)

        super.selectPrgPage(slot, newPage, memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            selectedBlock = value and 0x03
            updateState()
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
