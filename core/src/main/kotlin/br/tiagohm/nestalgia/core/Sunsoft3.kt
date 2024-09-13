package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_067

class Sunsoft3(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x800

    @Volatile private var irqEnabled = false
    @Volatile private var irqLatch = false
    @Volatile private var irqCounter = 0

    override fun initialize() {
        selectPrgPage(1, -1)
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter = (irqCounter - 1) and 0xFFFF

            if (irqCounter == 0xFFFF) {
                irqEnabled = false
                console.cpu.setIRQSource(EXTERNAL)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF800) {
            0x8800 -> selectChrPage(0, value)
            0x9800 -> selectChrPage(1, value)
            0xA800 -> selectChrPage(2, value)
            0xB800 -> selectChrPage(3, value)
            0xC800 -> {
                irqCounter = irqCounter and if (irqLatch) 0xFF00 else 0x00FF
                irqCounter = irqCounter or if (irqLatch) value else value shl 8
                irqLatch = !irqLatch
            }
            0xD800 -> {
                irqEnabled = value.bit4
                irqLatch = false
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0xE800 -> when (value and 0x03) {
                0 -> mirroringType = VERTICAL
                1 -> mirroringType = HORIZONTAL
                2 -> mirroringType = SCREEN_A_ONLY
                3 -> mirroringType = SCREEN_B_ONLY
            }
            0xF800 -> selectPrgPage(0, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqEnabled", irqEnabled)
        s.write("irqCounter", irqCounter)
        s.write("irqLatch", irqLatch)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqEnabled = s.readBoolean("irqEnabled")
        irqCounter = s.readInt("irqCounter")
        irqLatch = s.readBoolean("irqLatch")
    }
}
