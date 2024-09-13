package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_249

class Mapper249(console: Console) : MMC3(console) {

    @Volatile private var exReg = 0

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x5000, 0x5000, WRITE)
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        if (exReg.bit1) {
            val newPage =
                (page and 0x03) or (page shr 1 and 0x04) or (page shr 4 and 0x08) or (page shr 2 and 0x10) or (page shl 3 and 0x20) or (page shl 2 and 0xC0)
            super.selectChrPage(slot, newPage, memoryType)
        } else {
            super.selectChrPage(slot, page, memoryType)
        }
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        if (exReg.bit1) {
            val newPage = if (page < 0x20) {
                page and 0x01 or (page shr 3 and 0x02) or (page shr 1 and 0x04) or (page shl 2 and 0x18)
            } else {
                val p = page - 0x20
                p and 0x03 or (p shr 1 and 0x04) or (p shr 4 and 0x08) or (p shr 2 and 0x10) or (p shl 3 and 0x20) or (p shl 2 and 0xC0)
            }

            super.selectPrgPage(slot, newPage, memoryType)
        } else {
            super.selectPrgPage(slot, page, memoryType)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0x5000) {
            exReg = value
            updatePrgMapping()
            updateChrMapping()
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

        exReg = s.readInt("exReg")
    }
}
