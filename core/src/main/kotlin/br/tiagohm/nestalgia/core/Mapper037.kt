package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_037

@ExperimentalUnsignedTypes
class Mapper037 : MMC3() {

    private var selectedBlock: UByte = 0U

    override val registerStartAddress: UShort = 0x6000U

    override val registerEndAddress: UShort = 0xFFFFU

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        selectedBlock = 0U
        updateState()
    }

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, if (selectedBlock >= 4U) (page or 0x80U) else page, memoryType)
    }

    override fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType) {
        when {
            selectedBlock <= 2U -> super.selectPrgPage(slot, page and 0x07U, memoryType)
            selectedBlock.toUInt() == 3U -> super.selectPrgPage(slot, (page and 0x07U) or 0x08U, memoryType)
            selectedBlock.toUInt() == 7U -> super.selectPrgPage(slot, (page and 0x07U) or 0x20U, memoryType)
            selectedBlock >= 4U -> super.selectPrgPage(slot, (page and 0x0FU) or 0x10U, memoryType)
            else -> super.selectPrgPage(slot, page, memoryType)
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr < 0x8000U) {
            if (canWriteToWram) {
                selectedBlock = value and 0x07U
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

        selectedBlock = s.readUByte("selectedBlock") ?: 0U
    }
}