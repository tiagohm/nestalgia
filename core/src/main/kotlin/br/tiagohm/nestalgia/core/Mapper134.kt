package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_134

class Mapper134 : MMC3() {

    private var exReg: UByte = 0U

    override fun init() {
        super.init()

        addRegisterRange(0x6001U, 0x6001U, MemoryOperation.WRITE)
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        if (softReset) {
            exReg = 0U
            updateState()
        }
    }

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, (page and 0xFFU) or ((exReg.toUInt() and 0x20U) shl 3).toUShort(), memoryType)
    }

    override fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, (page and 0x1FU) or ((exReg.toUInt() and 0x02U) shl 4).toUShort(), memoryType)
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr.toInt() == 0x6001) {
            exReg = value
            updateState()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        exReg = s.readUByte("exReg") ?: 0U
    }
}