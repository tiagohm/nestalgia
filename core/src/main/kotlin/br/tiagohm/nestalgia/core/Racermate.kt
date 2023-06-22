package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_168
// TODO: Memory bad error.

class Racermate(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x1000

    override val chrRamSize = 0x10000

    override val saveRamSize = 0

    override val isForceChrBattery = true

    private var irqCounter = 0

    override fun initialize() {
        selectPrgPage(1, -1)
        selectChrPage(0, 0)
    }

    override fun clock() {
        irqCounter--

        if (irqCounter <= 0) {
            irqCounter = 1024
            console.cpu.setIRQSource(EXTERNAL)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xC000) {
            0x8000 -> {
                selectPrgPage(0, value shr 6 and 0x03)
                selectChrPage(1, value and 0x0F)
            }
            0xC000 -> {
                irqCounter = 1024
                console.cpu.clearIRQSource(EXTERNAL)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqCounter", irqCounter)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqCounter = s.readInt("irqCounter")
    }
}
