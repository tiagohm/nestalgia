package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_048

class TaitoTc0690(console: Console) : MMC3(console) {

    private var irqDelay = 0
    private var isFlintstones = false

    override fun initialize() {
        super.initialize()

        selectPrgPage(2, -2)
        selectPrgPage(3, -1)

        // This cart appears to behave differently (maybe not an identical mapper?)
        // IRQ seems to be triggered at a different timing (approx 100 cpu cycles before regular mapper 48 timings)
        isFlintstones = info.subMapperId == 255
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqDelay", irqDelay)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqDelay = s.readInt("irqDelay")
    }

    override fun triggerIrq() {
        //"The IRQ seems to trip a little later than it does on MMC3.  It looks like about a 4 CPU cycle delay from the normal MMC3 IRQ time."
        //A value of 6 removes the shaking from The Jetsons
        irqDelay = if (isFlintstones) 19 else 6
    }

    override fun clock() {
        if (irqDelay > 0) {
            irqDelay--

            if (irqDelay <= 0) {
                console.cpu.setIRQSource(EXTERNAL)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE003) {
            0x8000 -> selectPrgPage(0, value and 0x3F)
            0x8001 -> selectPrgPage(1, value and 0x3F)
            0x8002 -> {
                selectChrPage(0, value * 2)
                selectChrPage(1, value * 2 + 1)
            }
            0x8003 -> {
                selectChrPage(2, value * 2)
                selectChrPage(3, value * 2 + 1)
            }
            0xA000, 0xA001, 0xA002, 0xA003 -> selectChrPage(4 + (addr and 0x03), value)
            0xC000 -> {
                // Flintstones expects either $C000 or $C001 to clear the irq flag.
                console.cpu.clearIRQSource(EXTERNAL)
                irqReloadValue = (value xor 0xFF) + if (isFlintstones) 0 else 1
            }
            0xC001 -> {
                // Flintstones expects either $C000 or $C001 to clear the irq flag.
                console.cpu.clearIRQSource(EXTERNAL)
                irqCounter = 0
                irqReload = true
            }
            0xC002 -> irqEnabled = true
            0xC003 -> {
                irqEnabled = false
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0xE000 -> mirroringType = if (value.bit6) HORIZONTAL else VERTICAL
        }
    }
}
