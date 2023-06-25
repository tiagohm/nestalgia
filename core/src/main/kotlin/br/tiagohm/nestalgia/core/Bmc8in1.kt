package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_333

class Bmc8in1(console: Console) : MMC3(console) {

    private var reg = 0

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, reg and 0x0C shl 5 or (page and 0x7F), memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        if (reg.bit4) {
            super.selectPrgPage(slot, reg and 0x0C shl 2 or (page and 0x0F), memoryType)
        } else {
            selectPrgPage4x(0, reg and 0x0F shl 2, memoryType)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0x1000 != 0) {
            reg = value
            updateState()
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("reg", reg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        reg = s.readInt("reg")
    }
}
