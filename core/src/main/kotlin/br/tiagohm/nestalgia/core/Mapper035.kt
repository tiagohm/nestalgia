package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_035

class Mapper035(console: Console) : Mapper(console) {

    private var irqCounter = 0
    private var irqEnabled = false
    private val a12Watcher = A12RisingEdgeWatcher(console)

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override fun initialize() {
        selectPrgPage(3, -1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF007) {
            0x8000, 0x8001, 0x8002, 0x8003 -> selectPrgPage(addr and 0x03, value)
            0x9000, 0x9001, 0x9002, 0x9003,
            0x9004, 0x9005, 0x9006, 0x9007 -> selectChrPage(addr and 0x07, value)
            0xC002 -> {
                irqEnabled = false
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0xC003 -> irqEnabled = true
            0xC005 -> irqCounter = value
            0xD001 -> {
                mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
            }
        }
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        // MMC3-style A12 IRQ counter.
        // if (a12Watcher.updateVRAMAddress(addr, console.ppu.frameCycle) == RISE) {
        if (a12Watcher.isRisingEdge(addr)) {
            if (irqEnabled) {
                irqCounter--

                if (irqCounter == 0) {
                    irqEnabled = false
                    console.cpu.setIRQSource(EXTERNAL)
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
        s.write("a12Watcher", a12Watcher)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
        s.readSnapshotable("a12Watcher", a12Watcher, a12Watcher::reset)
    }
}
