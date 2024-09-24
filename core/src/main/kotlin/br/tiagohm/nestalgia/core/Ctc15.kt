package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE
import br.tiagohm.nestalgia.core.PrgMemoryType.WRAM

// https://www.nesdev.org/wiki/NES_2.0_Mapper_548

class Ctc15(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    @Volatile private var latch = 0x07
    @Volatile private var irqCounter = 0
    @Volatile private var irqEnabled = false

    override fun initialize() {
        addRegisterRange(0x4800, 0x4FFF, WRITE)
        addRegisterRange(0x5000, 0x57FF, WRITE)
        removeRegisterRange(0x8000, 0xFFFF, WRITE)

        selectChrPage(0, 0)
        selectPrgPage(0, latch xor 0x05)
        selectPrgPage(1, 0x03)

        addCpuMemoryMapping(0x6000, 0x7FFF, 0, WRAM)
    }

    override fun clock() {
        if (irqEnabled) {
            if (irqCounter == 23680) {
                console.cpu.setIRQSource(IRQSource.EXTERNAL)
            } else if (irqCounter == 24320) {
                console.cpu.clearIRQSource(IRQSource.EXTERNAL)
            }

            irqCounter++
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x5000) {
            latch = (addr shr 3 and 0x04) or (addr shr 2 and 0x03)
            irqEnabled = addr.bit2

            if (!irqEnabled) {
                irqCounter = 0
                console.cpu.clearIRQSource(IRQSource.EXTERNAL)
            }
        } else {
            selectPrgPage(0, latch xor 0x05)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("latch", latch)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        latch = s.readInt("latch")
        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
    }
}
