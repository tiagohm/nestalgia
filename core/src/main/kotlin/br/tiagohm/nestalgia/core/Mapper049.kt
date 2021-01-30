package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_049

@ExperimentalUnsignedTypes
class Mapper049 : MMC3() {

    private var selectedBlock: UByte = 0U
    private var prgReg: UByte = 0U
    private var prgMode49 = false

    override val registerStartAddress: UShort = 0x6000U

    override val registerEndAddress: UShort = 0xFFFFU

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        selectedBlock = 0U
        prgReg = 0U
        prgMode49 = false
    }

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, (page and 0x7FU) or (selectedBlock * 0x80U).toUShort(), memoryType)
    }

    override fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType) {
        super.selectPrgPage(
            slot,
            if (prgMode49) (page and 0x0FU) or (selectedBlock * 0x10U).toUShort() else (prgReg * 4U + slot).toUShort(),
            memoryType
        )
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr < 0x8000U) {
            if (canWriteToWram) {
                selectedBlock = (value shr 6) and 0x03U
                prgReg = (value shr 4) and 0x03U
                prgMode49 = value.bit0

                updateState()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("selectedBlock", selectedBlock)
        s.write("prgReg", prgReg)
        s.write("prgMode49", prgMode49)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        selectedBlock = s.readUByte("selectedBlock") ?: 0U
        prgReg = s.readUByte("prgReg") ?: 0U
        prgMode49 = s.readBoolean("prgMode49") ?: false
    }
}