package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_012

@ExperimentalUnsignedTypes
class Mapper012 : MMC3() {

    private var chrSelection: UByte = 0U

    override val forceMmc3RevAIrqs = true

    override fun init() {
        addRegisterRange(0x4020U, 0x5FFFU)

        super.init()
    }

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        if (slot < 4U && chrSelection.bit0) {
            // 0x0000 to 0x0FFF
            super.selectChrPage(slot, page or 0x100U, memoryType)
        } else if (slot >= 4U && chrSelection.bit4) {
            // 0x1000 to 0x1FFF
            super.selectChrPage(slot, page or 0x100U, memoryType)
        } else {
            super.selectChrPage(slot, page, memoryType)
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr <= 0x5FFFU) {
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

        chrSelection = s.readUByte("chrSelection") ?: 0U
    }
}