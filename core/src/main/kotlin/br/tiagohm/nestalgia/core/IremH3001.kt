package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.EXTERNAL
import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_065

class IremH3001(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    @Volatile private var irqEnabled = false
    @Volatile private var irqCounter = 0
    @Volatile private var irqReloadValue = 0

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 1)
        selectPrgPage(2, 0xFE)
        selectPrgPage(3, -1)
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter--

            if (irqCounter == 0) {
                irqEnabled = false
                console.cpu.setIRQSource(EXTERNAL)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            0x8000 -> selectPrgPage(0, value)
            0x9001 -> mirroringType = if (value.bit7) HORIZONTAL else VERTICAL
            0x9003 -> {
                irqEnabled = value.bit7
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0x9004 -> {
                irqCounter = irqReloadValue
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0x9005 -> irqReloadValue = irqReloadValue and 0x00FF or (value shl 8)
            0x9006 -> irqReloadValue = irqReloadValue and 0xFF00 or value
            0xA000 -> selectPrgPage(1, value)
            0xB000 -> selectChrPage(0, value)
            0xB001 -> selectChrPage(1, value)
            0xB002 -> selectChrPage(2, value)
            0xB003 -> selectChrPage(3, value)
            0xB004 -> selectChrPage(4, value)
            0xB005 -> selectChrPage(5, value)
            0xB006 -> selectChrPage(6, value)
            0xB007 -> selectChrPage(7, value)
            0xC000 -> selectPrgPage(2, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqEnabled", irqEnabled)
        s.write("irqCounter", irqCounter)
        s.write("irqReloadValue", irqReloadValue)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqEnabled = s.readBoolean("irqEnabled")
        irqCounter = s.readInt("irqCounter")
        irqReloadValue = s.readInt("irqReloadValue")
    }
}
