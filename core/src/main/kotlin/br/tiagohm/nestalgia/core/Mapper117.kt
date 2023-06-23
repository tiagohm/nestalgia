package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.A12StateChange.*
import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_117

class Mapper117(console: Console) : Mapper(console) {

    private var irqCounter = 0
    private var irqReloadValue = 0
    private var irqEnabled = false
    private var irqEnabledAlt = false
    private val a12Watcher = A12Watcher()

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override fun initialize() {
        selectPrgPage4x(0, -4)
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (a12Watcher.updateVRAMAddress(addr, console.ppu.frameCycle) == RISE) {
            if (irqEnabled && irqEnabledAlt && irqCounter > 0) {
                irqCounter--

                if (irqCounter == 0) {
                    console.cpu.setIRQSource(EXTERNAL)
                    irqEnabledAlt = false
                }
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0x8000, 0x8001, 0x8002, 0x8003 -> selectPrgPage(addr and 0x03, value)
            0xA000, 0xA001, 0xA002, 0xA003, 0xA004, 0xA005, 0xA006, 0xA007 -> selectChrPage(addr and 0x07, value)
            0xC001 -> irqReloadValue = value
            0xC002 -> console.cpu.clearIRQSource(EXTERNAL)
            0xC003 -> {
                irqCounter = irqReloadValue
                irqEnabledAlt = true
            }
            0xD000 -> mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
            0xE000 -> {
                irqEnabled = value.bit0
                console.cpu.clearIRQSource(EXTERNAL)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqCounter", irqCounter)
        s.write("irqReloadValue", irqReloadValue)
        s.write("irqEnabled", irqEnabled)
        s.write("irqEnabledAlt", irqEnabledAlt)
        s.write("a12Watcher", a12Watcher)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqCounter = s.readInt("irqCounter")
        irqReloadValue = s.readInt("irqReloadValue")
        irqEnabled = s.readBoolean("irqEnabled")
        irqEnabledAlt = s.readBoolean("irqEnabledAlt")
        s.readSnapshotable("a12Watcher", a12Watcher)
    }
}
