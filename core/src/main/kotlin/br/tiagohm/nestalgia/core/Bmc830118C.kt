package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_348

class Bmc830118C(console: Console) : MMC3(console) {

    private var reg = 0

    override fun reset(softReset: Boolean) {
        reg = 0
        super.reset(softReset)
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, reg and 0x0C shl 5 or (page and 0x7F), memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        if (reg and 0x0C == 0x0C) {
            if (slot == 0) {
                super.selectPrgPage(0, reg and 0x0C shl 2 or (page and 0x0F), memoryType)
                super.selectPrgPage(2, 0x32 or (page and 0x0F), memoryType)
            } else if (slot == 1) {
                super.selectPrgPage(1, reg and 0x0C shl 2 or (page and 0x0F), memoryType)
                super.selectPrgPage(3, 0x32 or (page and 0x0F), memoryType)
            }
        } else {
            super.selectPrgPage(slot, reg and 0x0C shl 2 or (page and 0x0F), memoryType)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            reg = value
            updateState()
        } else {
            super.writeRegister(addr, value)
        }
    }
}
