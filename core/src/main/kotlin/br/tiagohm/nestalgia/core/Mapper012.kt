package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_012

class Mapper012 : MMC3() {

    private var chrSelection = 0

    override val forceMmc3RevAIrqs = true

    override fun initialize() {
        addRegisterRange(0x4020, 0x5FFF)

        super.initialize()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (slot < 4 && chrSelection.bit0) {
            // 0x0000 to 0x0FFF
            super.selectChrPage(slot, page or 0x100, memoryType)
        } else if (slot >= 4 && chrSelection.bit4) {
            // 0x1000 to 0x1FFF
            super.selectChrPage(slot, page or 0x100, memoryType)
        } else {
            super.selectChrPage(slot, page, memoryType)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr <= 0x5FFF) {
            chrSelection = value
            updateState()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chrSelection", chrSelection)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        chrSelection = s.readInt("chrSelection")
    }
}
