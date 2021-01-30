package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_044

@ExperimentalUnsignedTypes
class Mapper044 : MMC3() {

    private var selectedBlock: UByte = 0U

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        selectedBlock = 0U
        updateState()
    }

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        super.selectChrPage(
            slot,
            (page and if (selectedBlock <= 5U) 0x7FU else 0xFFU) or (selectedBlock * 0x80U).toUShort(),
            memoryType
        )
    }

    override fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType) {
        super.selectPrgPage(
            slot,
            (page and if (selectedBlock <= 5U) 0x0FU else 0x1FU) or (selectedBlock * 0x10U).toUShort(),
            memoryType
        )
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if ((addr.toUInt() and 0xE001U) == 0xA001U) {
            selectedBlock = value and 0x07U

            if (selectedBlock.toUInt() == 7U) {
                selectedBlock = 6U
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

        selectedBlock = s.readUByte("selectedBlock") ?: 0U
    }
}