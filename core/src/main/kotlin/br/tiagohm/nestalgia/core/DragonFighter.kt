package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_292

class DragonFighter(console: Console) : MMC3(console) {

    private val exRegs = IntArray(3)

    override val allowRegisterRead = true

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x6000, 0x6FFF, MemoryAccessType.READ_WRITE)
        removeRegisterRange(0x8000, 0xFFFF, MemoryAccessType.READ)
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        when (slot) {
            0 -> selectChrPage2x(0, ((page shr 1) xor exRegs[1]) shl 1)
            2 -> selectChrPage2x(1, ((page shr 1) or ((exRegs[2] and 0x40) shl 1)) shl 1)
            4 -> selectChrPage4x(1, exRegs[2] and 0x3F shl 2)
        }
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        if (slot == 0) {
            super.selectPrgPage(slot, exRegs[0] and 0x1F, memoryType)
        } else {
            super.selectPrgPage(slot, page, memoryType)
        }
    }

    override fun readRegister(addr: Int): Int {
        if (!addr.bit0) {
            if (exRegs[0] and 0xE0 == 0xC0) {
                exRegs[1] = console.memoryManager.peek(0x6A)
            } else {
                exRegs[2] = console.memoryManager.peek(0xFF)
            }

            updateState()
        }

        return 0
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (!addr.bit0) {
                exRegs[0] = value
                updateState()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exRegs", exRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("exRegs", exRegs)
    }
}
