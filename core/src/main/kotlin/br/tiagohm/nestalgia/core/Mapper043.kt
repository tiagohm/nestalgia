package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_043

class Mapper043(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x4020

    override val registerEndAddress = 0xFFFF

    private var reg = 0
    private var swap = false
    private var irqCounter = 0
    private var irqEnabled = false

    override fun initialize() {
        updateState()
        addCpuMemoryMapping(0x5000, 0x5FFF, 8, ROM)
        selectPrgPage(0, 1)
        selectPrgPage(1, 0)
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

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateState() {
        addCpuMemoryMapping(0x6000, 0x7FFF, if (swap) 0 else 2, ROM)
        selectPrgPage(2, reg)
        selectPrgPage(3, if (swap) 8 else 9)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF1FF) {
            0x4022 -> {
                reg = LUT[value and 0x07]
                updateState()
            }
            0x4120 -> {
                swap = value.bit0
                updateState()
            }
            0x8122,
            0x4122 -> {
                irqEnabled = value.bit0
                console.cpu.clearIRQSource(EXTERNAL)
                irqCounter = 0
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("reg", reg)
        s.write("swap", swap)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        reg = s.readInt("reg")
        swap = s.readBoolean("swap")
        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")

        updateState()
    }

    companion object {

        @JvmStatic private val LUT = intArrayOf(4, 3, 5, 3, 6, 3, 7, 3)
    }
}
