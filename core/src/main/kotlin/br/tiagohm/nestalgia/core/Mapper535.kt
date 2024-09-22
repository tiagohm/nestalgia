package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE
import br.tiagohm.nestalgia.core.PrgMemoryType.ROM
import br.tiagohm.nestalgia.core.PrgMemoryType.WRAM

// https://www.nesdev.org/wiki/NES_2.0_Mapper_535

class Mapper535(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val workRamSize = 0x2000

    override val workRamPageSize = 0x2000

    override val isForceWorkRamSize = true

    @Volatile private var irqEnabled = false
    @Volatile private var irqCounter = 0

    override fun initialize() {
        selectPrgPage4x(0, 3 shl 2)
        selectChrPage(0, 0)
        addCpuMemoryMapping(0x6000, 0x7FFF, 0, ROM)
        addCpuMemoryMapping(0xB800, 0xD7FF, 0, WRAM)

        removeRegisterRange(0x8000, 0xDFFF, READ_WRITE)
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter++

            if (irqCounter == 7560) {
                console.cpu.setIRQSource(IRQSource.EXTERNAL)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF000) {
            0xE000 -> {
                irqEnabled = value.bit1
                irqCounter = 0
                console.cpu.clearIRQSource(IRQSource.EXTERNAL)
            }
            0xF000 -> addCpuMemoryMapping(0x6000, 0x7FFF, value, ROM)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqEnabled", irqEnabled)
        s.write("irqCounter", irqCounter)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqEnabled = s.readBoolean("irqEnabled")
        irqCounter = s.readInt("irqCounter")
    }
}
