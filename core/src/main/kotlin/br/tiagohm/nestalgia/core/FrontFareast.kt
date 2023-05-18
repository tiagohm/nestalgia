package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.EXTERNAL
import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.SCREEN_A_ONLY
import br.tiagohm.nestalgia.core.MirroringType.SCREEN_B_ONLY
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

abstract class FrontFareast : Mapper() {

    protected var irqCounter = 0
    protected var irqEnabled = false
    protected var ffeAltMode = true

    final override val prgPageSize = 0x2000U

    final override val chrPageSize = 0x400U

    final override val chrRamSize = 0x8000U

    final override val registerStartAddress: UShort = 0x42FEU

    final override val registerEndAddress: UShort = 0x4517U

    final override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
        s.write("ffeAltMode", ffeAltMode)
    }

    final override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqCounter = s.readInt("irqCounter") ?: 0
        irqEnabled = s.readBoolean("irqEnabled") ?: false
        ffeAltMode = s.readBoolean("ffeAltMode") ?: true
    }

    final override fun processCpuClock() {
        if (irqEnabled) {
            irqCounter++

            if (irqCounter == 0) {
                console.cpu.setIRQSource(EXTERNAL)
                irqEnabled = false
            }
        }
    }

    protected abstract fun handleWriteRegister(addr: UShort, value: UByte)

    final override fun writeRegister(addr: UShort, value: UByte) {
        when (addr.toInt()) {
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
                irqCounter = (irqCounter and 0xFF00) or value.toInt()
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0x4503 -> {
                irqCounter = (irqCounter and 0x00FF) or (value.toInt() shl 8)
                irqEnabled = true
                console.cpu.clearIRQSource(EXTERNAL)
            }
            else -> handleWriteRegister(addr, value)
        }
    }
}
