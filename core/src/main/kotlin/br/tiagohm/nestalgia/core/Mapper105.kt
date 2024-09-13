package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_105

class Mapper105(console: Console) : MMC1(console) {

    @Volatile private var initState = 0
    @Volatile private var irqCounter = 0L
    @Volatile private var irqEnabled = false

    override val dipSwitchCount = 4

    override fun initialize() {
        super.initialize()

        initState = 0
        irqCounter = 0L
        irqEnabled = false
        stateA000 = stateA000 or 0x10 // Set I bit to 1
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter++

            val maxCounter = 0x20000000 or (dipSwitches shl 25)

            if (irqCounter >= maxCounter) {
                console.cpu.setIRQSource(EXTERNAL)
                irqEnabled = false
            }
        }
    }

    override fun updateState() {
        if (initState == 0 && !stateA000.bit4) {
            initState = 1
        } else if (initState == 1 && stateA000.bit4) {
            initState = 2
        }

        if (stateA000.bit4) {
            irqEnabled = false
            irqCounter = 0
            console.cpu.clearIRQSource(EXTERNAL)
        } else {
            irqEnabled = true
        }

        mirroringType = when (state8000 and 3) {
            0 -> SCREEN_A_ONLY
            1 -> SCREEN_B_ONLY
            2 -> VERTICAL
            else -> HORIZONTAL
        }

        val access = if (stateE000.bit4) NO_ACCESS else READ_WRITE
        addCpuMemoryMapping(0x6000, 0x7FFF, 0, if (hasBattery) SRAM else WRAM, access)

        if (initState == 2) {
            if (stateA000.bit3) {
                // MMC1 mode
                val prgReg = (stateE000 and 0x07) or 0x08

                if (state8000.bit3) {
                    if (state8000.bit2) {
                        selectPrgPage(0, prgReg)
                        selectPrgPage(1, 0x0F)
                    } else {
                        selectPrgPage(0, 0x08)
                        selectPrgPage(1, prgReg)
                    }
                } else {
                    selectPrgPage2x(0, prgReg and 0xFE)
                }
            } else {
                selectPrgPage2x(0, stateA000 and 0x06)
            }
        } else {
            selectPrgPage2x(0, 0)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("initState", initState)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        initState = s.readInt("initState")
        irqCounter = s.readLong("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
    }
}
