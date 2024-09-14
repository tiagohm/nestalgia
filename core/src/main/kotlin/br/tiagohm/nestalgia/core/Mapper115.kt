package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_115

class Mapper115(console: Console) : MMC3(console) {

    @Volatile private var prgReg = 0
    @Volatile private var chrReg = 0
    @Volatile private var protectionReg = 0

    override val allowRegisterRead = true

    override fun initialize() {
        addRegisterRange(0x4100, 0x7FFF, WRITE)
        addRegisterRange(0x5000, 0x5FFF, READ)
        removeRegisterRange(0x8000, 0xFFFF, READ)

        super.initialize()
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, page or chrReg shl 8, memoryType)
    }

    override fun updateState() {
        super.updateState()

        if (prgReg.bit7) {
            if (prgReg.bit5) {
                selectPrgPage4x(0, prgReg and 0x0F shr 1 shl 2) // TODO: ESTÁ CERTO SHR E SHL
            } else {
                val page = prgReg and 0x0F shl 1
                selectPrgPage2x(0, page)
                selectPrgPage2x(1, page)
            }
        }
    }

    override fun readRegister(addr: Int) = protectionReg

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (addr == 0x5080) {
                protectionReg = value
            } else {
                if (addr.bit0) {
                    chrReg = value and 0x01
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

        prgReg = s.readInt("prgReg")
        chrReg = s.readInt("chrReg")
    }
}
