package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_106

class Mapper106(console: Console) : Mapper(console) {

    @Volatile private var irqCounter = 0 // unsigned 16-bits
    @Volatile private var irqEnabled = false

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override fun initialize() {
        selectPrgPage(0, -1)
        selectPrgPage(1, -1)
        selectPrgPage(2, -1)
        selectPrgPage(3, -1)
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter = (irqCounter + 1) and 0xFFFF

            if (irqCounter == 0) {
                console.cpu.setIRQSource(EXTERNAL)
                irqEnabled = false
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0x0F) {
            0, 2 -> selectChrPage(addr and 0x0F, value and 0xFE)
            1, 3 -> selectChrPage(addr and 0x0F, value or 0x01)
            4, 5, 6, 7 -> selectChrPage(addr and 0x0F, value)
            8, 0x0B -> selectPrgPage((addr and 0x0F) - 8, (value and 0x0F) or 0x10)
            9, 0x0A -> selectPrgPage((addr and 0x0F) - 8, value and 0x1F)
            0x0D -> {
                irqEnabled = false
                irqCounter = 0
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0x0E -> {
                irqCounter = (irqCounter and 0xFF00) or value
            }
            0x0F -> {
                irqCounter = (irqCounter and 0xFF) or (value shl 8)
                irqEnabled = true
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
