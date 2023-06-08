package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_044

class Mapper044 : MMC3() {

    private var selectedBlock = 0

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        selectedBlock = 0
        updateState()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(
            slot,
            (page and if (selectedBlock <= 5) 0x7F else 0xFF) or (selectedBlock * 0x80),
            memoryType,
        )
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(
            slot,
            (page and if (selectedBlock <= 5) 0x0F else 0x1F) or (selectedBlock * 0x10),
            memoryType,
        )
    }

    override fun writeRegister(addr: Int, value: Int) {
        if ((addr and 0xE001) == 0xA001) {
            selectedBlock = value and 0x07

            if (selectedBlock == 7) {
                selectedBlock = 6
            }
        }

        super.writeRegister(addr, value)
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
