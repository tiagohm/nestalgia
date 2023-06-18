package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_073

class VRC3(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    private var irqEnableOnAck = false
    private var irqEnabled = false
    private var smallCounter = false
    private var irqReload = 0
    private var irqCounter = 0

    override fun initialize() {
        selectPrgPage(1, -1)
        selectChrPage(0, 0)
    }

    override fun clock() {
        if (irqEnabled) {
            if (smallCounter) {
                var counter = (irqCounter + 1) and 0xFF

                if (counter == 0) {
                    counter = irqReload and 0xFF
                    console.cpu.setIRQSource(EXTERNAL)
                }

                irqCounter = irqCounter and 0xFF00 or counter
            } else {
                irqCounter++

                if (irqCounter and 0xFFFF == 0) {
                    irqCounter = irqReload
                    console.cpu.setIRQSource(EXTERNAL)
                }
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF000) {
            0x8000 -> irqReload = irqReload and 0xFFF0 or (value and 0x0F)
            0x9000 -> irqReload = irqReload and 0xFF0F or (value and 0x0F shl 4)
            0xA000 -> irqReload = irqReload and 0xF0FF or (value and 0x0F shl 8)
            0xB000 -> irqReload = irqReload and 0x0FFF or (value and 0x0F shl 12)
            0xC000 -> {
                irqEnabled = value.bit1

                if (irqEnabled) {
                    irqCounter = irqReload
                }

                smallCounter = value.bit2
                irqEnableOnAck = value.bit0

                console.cpu.clearIRQSource(EXTERNAL)
            }
            0xD000 -> {
                console.cpu.clearIRQSource(EXTERNAL)
                irqEnabled = irqEnableOnAck
            }
            0xF000 -> selectPrgPage(0, value and 0x07)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqEnableOnAck", irqEnableOnAck)
        s.write("irqEnabled", irqEnabled)
        s.write("smallCounter", smallCounter)
        s.write("irqReload", irqReload)
        s.write("irqCounter", irqCounter)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqEnableOnAck = s.readBoolean("irqEnableOnAck")
        irqEnabled = s.readBoolean("irqEnabled")
        smallCounter = s.readBoolean("smallCounter")
        irqReload = s.readInt("irqReload")
        irqCounter = s.readInt("irqCounter")
    }
}
