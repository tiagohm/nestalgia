package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*

class Mapper040(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    @Volatile private var irqCounter = 0

    override fun initialize() {
        addCpuMemoryMapping(0x6000, 0x7FFF, 6, PrgMemoryType.ROM)
        selectPrgPage(0, 4)
        selectPrgPage(1, 5)
        selectPrgPage(3, 7)
        selectChrPage(0, 0)
    }

    override fun clock() {
        if (irqCounter > 0) {
            irqCounter--

            if (irqCounter == 0) {
                console.cpu.setIRQSource(EXTERNAL)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE000) {
            0x8000 -> {
                irqCounter = 0
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0xA000 -> {
                irqCounter = 4096
            }
            0xE000 -> {
                selectPrgPage(2, value)
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
