package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_222

class Mapper222(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    private var irqCounter = 0
    private val a12Watcher = A12RisingEdgeWatcher(console)

    override fun initialize() {
        selectPrgPage2x(1, -2)
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (a12Watcher.isRisingEdge(addr)) {
            if (irqCounter > 0) {
                irqCounter++

                if (irqCounter >= 240) {
                    console.cpu.setIRQSource(EXTERNAL)
                    irqCounter = 0
                }
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF003) {
            0x8000 -> selectPrgPage(0, value)
            0x9000 -> mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
            0xA000 -> selectPrgPage(1, value)
            0xB000 -> selectChrPage(0, value)
            0xB002 -> selectChrPage(1, value)
            0xC000 -> selectChrPage(2, value)
            0xC002 -> selectChrPage(3, value)
            0xD000 -> selectChrPage(4, value)
            0xD002 -> selectChrPage(5, value)
            0xE000 -> selectChrPage(6, value)
            0xE002 -> selectChrPage(7, value)
            0xF000 -> {
                irqCounter = value
                console.cpu.clearIRQSource(EXTERNAL)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqCounter", irqCounter)
        s.write("a12Watcher", a12Watcher)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqCounter = s.readInt("irqCounter")
        s.readSnapshotable("a12Watcher", a12Watcher)
    }
}
