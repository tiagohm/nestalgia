package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_047

@ExperimentalUnsignedTypes
class Mapper047 : MMC3() {

    private var selectedBlock: UByte = 0U

    override val registerStartAddress: UShort = 0x6000U

    override val registerEndAddress: UShort = 0xFFFFU

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, (page and 0x7FU) or if (selectedBlock.isOne) 0x80U else 0x00U, memoryType)
    }

    override fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, (page and 0x0FU) or if (selectedBlock.isOne) 0x10U else 0x00U, memoryType)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr < 0x8000U) {
            if (canWriteToWram) {
                selectedBlock = value and 0x01U
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