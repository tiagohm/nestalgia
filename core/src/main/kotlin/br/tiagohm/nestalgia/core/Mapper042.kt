package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_042

class Mapper042(console: Console) : Mapper(console) {

    private var irqCounter = 0 // unsigned 16-bits
    private var irqEnabled = false
    private var prgReg = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, -4)
        selectPrgPage(1, -3)
        selectPrgPage(2, -2)
        selectPrgPage(3, -1)
        selectChrPage(0, 0)

        updateState()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateState() {
        addCpuMemoryMapping(0x6000, 0x7FFF, prgReg and 0x0F, ROM)
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter++

            if (irqCounter >= 0x8000) {
                irqCounter -= 0x8000
            }

            if (irqCounter >= 0x6000) {
                console.cpu.setIRQSource(EXTERNAL)
            } else {
                console.cpu.clearIRQSource(EXTERNAL)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE003) {
            0x8000 -> {
                if (mChrRomSize > 0) {
                    selectChrPage(0, value and 0x0F)
                }
            }
            0xE000 -> {
                prgReg = value and 0x0F
                updateState()
            }
            0xE001 -> {
                mirroringType = if (value.bit3) HORIZONTAL else VERTICAL
            }
            0xE002 -> {
                irqEnabled = value == 0x02

                if (!irqEnabled) {
                    console.cpu.clearIRQSource(EXTERNAL)
                    irqCounter = 0
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
        s.write("prgReg", prgReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
        prgReg = s.readInt("prgReg")

        updateState()
    }
}
