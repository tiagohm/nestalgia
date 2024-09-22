package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.PrgMemoryType.ROM

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_304

class Smb2j(console: Console) : Mapper(console) {

    override val prgPageSize = 0x1000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x4122

    override val registerEndAddress = 0x4122

    @Volatile private var irqCounter = 0
    @Volatile private var irqEnabled = false

    override fun initialize() {
        selectPrgPage4x(0, 0)
        selectPrgPage4x(1, 4)
        selectChrPage(0, 0)

        if (mPrgSize >= 0x10000) {
            addRegisterRange(0x4022, 0x4022, MemoryAccessType.WRITE)
        }

        addCpuMemoryMapping(0x5000, 0x5FFF, prgPageCount - 3, ROM)
        addCpuMemoryMapping(0x6000, 0x6FFF, prgPageCount - 2, ROM)
        addCpuMemoryMapping(0x7000, 0x7FFF, prgPageCount - 1, ROM)
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter = (irqCounter + 1) and 0xFFF

            if (irqCounter == 0) {
                irqEnabled = false
                console.cpu.setIRQSource(IRQSource.EXTERNAL)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr == 0x4022) {
            selectPrgPage4x(0, value and 0x01 shl 2)
            selectPrgPage4x(1, (value and 0x01 shl 2) + 4)
        } else if (addr == 0x4122) {
            irqEnabled = (value and 0x03) != 0
            irqCounter = 0
            console.cpu.clearIRQSource(IRQSource.EXTERNAL)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
    }
}
