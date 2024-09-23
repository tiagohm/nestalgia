package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE

// https://www.nesdev.org/wiki/NES_2.0_Mapper_550

class Mapper550(console: Console) : MMC1(console) {

    @Volatile private var selectedBlock = 0
    @Volatile private var latch = 0

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x7000, 0x7FFF, WRITE)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, (selectedBlock shl 2) or (page and 0x07), memoryType)
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, (selectedBlock shl 2 and 0x18) or (page and 0x07), memoryType)
    }

    override fun updateState() {
        if (selectedBlock and 0x06 == 0x06) {
            super.updateState()
        } else {
            selectPrgPage2x(0, ((selectedBlock shl 1) or (latch shr 4)) shl 1)
            selectChrPage2x(0, ((selectedBlock shl 1 and 0x0C) or (latch and 0x03)) shl 1)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            writePrgRam(addr, value)

            if (!selectedBlock.bit3) {
                selectedBlock = addr and 0x0F
                updateState()
            }
        } else {
            latch = value

            if (selectedBlock and 0x06 == 0x06) {
                super.writeRegister(addr, value)
            }

            updateState()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("selectedBlock", selectedBlock)
        s.write("latch", latch)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        selectedBlock = s.readInt("selectedBlock")
        latch = s.readInt("latch")
    }
}
