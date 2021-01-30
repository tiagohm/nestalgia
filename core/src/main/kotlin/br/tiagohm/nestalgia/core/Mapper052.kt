package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_052

@ExperimentalUnsignedTypes
class Mapper052 : MMC3() {

    private var extraReg: UByte = 0U

    override val registerStartAddress: UShort = 0x6000U

    override val registerEndAddress: UShort = 0xFFFFU

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        extraReg = 0U
        updateState()
    }

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        val p = if (extraReg.bit6)
            (page and 0x7FU) or (((extraReg.toUInt() and 0x04U) or ((extraReg.toUInt() shr 4) and 0x03U)) shl 7).toUShort()
        else
            (page and 0xFFU) or (((extraReg.toUInt() and 0x04U) or ((extraReg.toUInt() shr 4) and 0x02U)) shl 7).toUShort()

        super.selectChrPage(slot, p, memoryType)
    }

    override fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType) {
        val p = if (extraReg.bit3)
            (page and 0x0FU) or ((extraReg and 0x07U).toUInt() shl 4).toUShort()
        else
            (page and 0x1FU) or ((extraReg and 0x06U).toUInt() shl 4).toUShort()

        super.selectPrgPage(slot, p, memoryType)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr < 0x8000U) {
            if (canWriteToWram) {
                // Bit 7: 1 = Disable multicart register and enable RAM 0 = Allow further writes to multicart register
                if (!extraReg.bit7) {
                    extraReg = value
                    updateState()
                } else {
                    writePrgRam(addr, value)
                }
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("extraReg", extraReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        extraReg = s.readUByte("extraReg") ?: 0U
    }
}