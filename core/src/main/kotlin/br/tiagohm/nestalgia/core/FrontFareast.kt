package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*

abstract class FrontFareast(console: Console) : Mapper(console) {

    protected var irqCounter = 0 // unsigned 16 bits
    protected var irqEnabled = false
    protected var ffeAltMode = true

    final override val prgPageSize = 0x2000

    final override val chrPageSize = 0x400

    final override val chrRamSize = 0x8000

    final override val registerStartAddress = 0x42FE

    final override val registerEndAddress = 0x4517

    final override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
        s.write("ffeAltMode", ffeAltMode)
    }

    final override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
        ffeAltMode = s.readBoolean("ffeAltMode", true)
    }

    final override fun processCpuClock() {
        if (irqEnabled) {
            irqCounter = (irqCounter + 1) and 0xFFFF

            if (irqCounter == 0) {
                console.cpu.setIRQSource(EXTERNAL)
                irqEnabled = false
            }
        }
    }

    protected abstract fun internalWriteRegister(addr: Int, value: Int)

    final override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0x42FE -> {
                ffeAltMode = !value.bit7
                mirroringType = if (value.bit4) SCREEN_B_ONLY else SCREEN_A_ONLY
            }
            0x42FF -> {
                mirroringType = if (value.bit4) HORIZONTAL else VERTICAL
            }
            0x4501 -> {
                irqEnabled = false
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0x4502 -> {
                irqCounter = (irqCounter and 0xFF00) or value
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0x4503 -> {
                irqCounter = (irqCounter and 0x00FF) or (value shl 8)
                irqEnabled = true
                console.cpu.clearIRQSource(EXTERNAL)
            }
            else -> internalWriteRegister(addr, value)
        }
    }
}
