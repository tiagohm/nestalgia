package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_325

class MaliSB(console: Console) : MMC3(console) {

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, page and 0xDD or (page and 0x20 shr 4) or (page and 0x02 shl 4), memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, page and 0x03 or (page and 0x08 shr 1) or (page and 0x04 shl 1), memoryType)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val newAddr = if (addr >= 0xC000) {
            addr and 0xFFFE or (addr shr 2 and 0x01) or (addr shr 3 and 0x01)
        } else {
            addr and 0xFFFE or (addr shr 3 and 0x01)
        }

        super.writeRegister(newAddr, value)
    }
}
