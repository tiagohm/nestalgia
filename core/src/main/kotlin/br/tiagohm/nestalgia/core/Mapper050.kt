package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_050

class Mapper050(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x4020

    override val registerEndAddress = 0x5FFF

    private var irqCounter = 0
    private var irqEnabled = false

    override fun initialize() {
        addCpuMemoryMapping(0x6000, 0x7FFF, 0x0F, ROM)
        selectPrgPage(0, 0x08)
        selectPrgPage(1, 0x09)
        selectPrgPage(3, 0x0B)
        selectChrPage(0, 0)
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter++

            if (irqCounter >= 4096) {
                irqEnabled = false
                console.cpu.setIRQSource(EXTERNAL)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0x4120) {
            0x4020 -> selectPrgPage(2, value and 0x08 or (value and 0x01 shl 2) or (value and 0x06 shr 1))
            0x4120 -> if (value.bit0) {
                irqEnabled = true
            } else {
                console.cpu.clearIRQSource(EXTERNAL)
                irqCounter = 0
                irqEnabled = false
            }
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
