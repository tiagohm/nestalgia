package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_052

class Mapper052(console: Console) : MMC3(console) {

    @Volatile private var extraReg = 0

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override val chrRamPageSize
        get() = if (data.info.subMapperId == 13) 0x400 else 0

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        extraReg = 0
        updateState()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        val p = if (extraReg.bit6)
            (page and 0x7F) or (((extraReg and 0x20 shr 3) or (extraReg and 0x10 shr 4) or (extraReg and 0x04 shr 1)) shl 7)
        else
            (page and 0xFF) or (((extraReg and 0x20 shr 3) or (extraReg and 0x10 shr 4)) shl 7)

        if (data.info.subMapperId == 13 && (extraReg and 3 == 3)) {
            super.selectChrPage(slot, p, ChrMemoryType.RAM)
        } else {
            super.selectChrPage(slot, p, memoryType)
        }
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        val p = if (extraReg.bit3)
            (page and 0x0F) or (extraReg and 0x07 shl 4)
        else
            (page and 0x1F) or (extraReg and 0x06 shl 4)

        super.selectPrgPage(slot, p, memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
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

        extraReg = s.readInt("extraReg")
    }
}
