package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_115

@ExperimentalUnsignedTypes
class Mapper115 : MMC3() {

    private var prgReg: UByte = 0U
    private var chrReg: UByte = 0U
    private var protectionReg: UByte = 0U

    override val allowRegisterRead = true

    override fun init() {
        addRegisterRange(0x4100U, 0x7FFFU, MemoryOperation.WRITE)
        addRegisterRange(0x5000U, 0x5FFFU, MemoryOperation.READ)
        removeRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.READ)

        super.init()
    }

    override fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, page or (chrReg.toUInt() shl 8).toUShort(), memoryType)
    }

    override fun updateState() {
        super.updateState()

        if (prgReg.bit7) {
            if (prgReg.bit5) {
                selectPrgPage4x(0U, (((prgReg and 0x0FU).toUInt() shr 1) shl 2).toUShort())
            } else {
                val page = ((prgReg and 0x0FU).toUInt() shl 1).toUShort()
                selectPrgPage2x(0U, page)
                selectPrgPage2x(1U, page)
            }
        }
    }

    override fun readRegister(addr: UShort) = protectionReg

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr < 0x8000U) {
            if (addr.toInt() == 0x5080) {
                protectionReg = value
            } else {
                if ((addr.toInt() and 0x01) == 0x01) {
                    chrReg = value and 0x01U
                } else {
                    prgReg = value
                }

                updateState()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
        s.write("chrReg", chrReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readUByte("prgReg") ?: 0U
        chrReg = s.readUByte("chrReg") ?: 0U
    }
}