package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_056
// https://wiki.nesdev.com/w/index.php/INES_Mapper_142

class Kaiser202(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    @Volatile private var irqReloadValue = 0
    @Volatile private var irqCounter = 0
    @Volatile private var irqEnabled = false
    @Volatile private var selectedReg = 0
    private val prgRegs = IntArray(4)

    override fun initialize() {
        selectPrgPage(3, -1)
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter++

            if (irqCounter == 0xFFFF) {
                irqCounter = irqReloadValue
                console.cpu.setIRQSource(EXTERNAL)
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xF000) {
            0x8000 -> irqReloadValue = irqReloadValue and 0xFFF0 or (value and 0x0F)
            0x9000 -> irqReloadValue = irqReloadValue and 0xFF0F or (value and 0x0F shl 4)
            0xA000 -> irqReloadValue = irqReloadValue and 0xF0FF or (value and 0x0F shl 8)
            0xB000 -> irqReloadValue = irqReloadValue and 0x0FFF or (value and 0x0F shl 12)
            0xC000 -> {
                irqEnabled = value != 0

                if (irqEnabled) {
                    irqCounter = irqReloadValue
                }

                console.cpu.clearIRQSource(EXTERNAL)
            }
            0xD000 -> console.cpu.clearIRQSource(EXTERNAL)
            0xE000 -> selectedReg = (value and 0x0F) - 1
            0xF000 -> {
                if (selectedReg < 3) {
                    prgRegs[selectedReg] = prgRegs[selectedReg] and 0x10 or (value and 0x0F)
                } else if (selectedReg < 4) {
                    // For Kaiser7032 (Mapper 142).
                    prgRegs[selectedReg] = value
                    addCpuMemoryMapping(0x6000, 0x7FFF, value, ROM, READ_WRITE)
                }

                when (addr and 0xFC00) {
                    0xF000 -> {
                        val bank = addr and 0x03

                        if (bank < 3) {
                            prgRegs[bank] = value and 0x10 or (prgRegs[bank] and 0x0F)
                        }
                    }
                    0xF800 -> mirroringType = if (value.bit0) VERTICAL else HORIZONTAL
                    0xFC00 -> selectChrPage(addr and 0x07, value)
                }

                selectPrgPage(0, prgRegs[0])
                selectPrgPage(1, prgRegs[1])
                selectPrgPage(2, prgRegs[2])
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqReloadValue", irqReloadValue)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
        s.write("selectedReg", selectedReg)
        s.write("prgRegs", prgRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqReloadValue = s.readInt("irqReloadValue")
        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
        selectedReg = s.readInt("selectedReg")
        s.readIntArray("prgRegs", prgRegs)

        addCpuMemoryMapping(0x6000, 0x7FFF, prgRegs[3], ROM, READ_WRITE)
    }
}
