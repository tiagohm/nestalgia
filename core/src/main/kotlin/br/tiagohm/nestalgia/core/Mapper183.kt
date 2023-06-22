package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_183

class Mapper183(console: Console) : Mapper(console) {

    private val chrRegs = IntArray(8)
    private var prgReg = 0
    private var irqCounter = 0
    private var irqScaler = 0
    private var irqEnabled = false
    private var needIrq = false

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        updatePrg()
    }

    private fun updatePrg() {
        addCpuMemoryMapping(0x6000, 0x7FFF, prgReg, ROM)
        selectPrgPage(3, -1)
    }

    override fun clock() {
        if (needIrq) {
            console.cpu.setIRQSource(EXTERNAL)
            needIrq = false
        }

        irqScaler++

        if (irqScaler == 114) {
            irqScaler = 0

            if (irqEnabled) {
                irqCounter = (irqCounter + 1) and 0xFF

                if (irqCounter == 0) {
                    needIrq = true
                }
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0xF800 == 0x6800) {
            prgReg = addr and 0x3F
            updatePrg()
        } else if (addr and 0xF80C in 0xB000..0xE00C) {
            val slot = (addr shr 11) - 6 or (addr shr 3) and 0x07
            chrRegs[slot] = chrRegs[slot] and (0xF0 shr (addr and 0x04)) or (value and 0x0F shl (addr and 0x04))
            selectChrPage(slot, chrRegs[slot])
        } else when (addr and 0xF80C) {
            0x8800 -> selectPrgPage(0, value)
            0xA800 -> selectPrgPage(1, value)
            0xA000 -> selectPrgPage(2, value)
            0x9800 -> when (value and 0x03) {
                0 -> mirroringType = VERTICAL
                1 -> mirroringType = HORIZONTAL
                2 -> mirroringType = SCREEN_A_ONLY
                3 -> mirroringType = SCREEN_B_ONLY
            }
            0xF000 -> irqCounter = irqCounter and 0xF0 or (value and 0x0F)
            0xF004 -> irqCounter = irqCounter and 0x0F or (value and 0x0F shl 4)
            0xF008 -> {
                irqEnabled = value > 0

                if (!irqEnabled) {
                    irqScaler = 0
                }

                console.cpu.clearIRQSource(EXTERNAL)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
        s.write("irqCounter", irqCounter)
        s.write("irqScaler", irqScaler)
        s.write("irqEnabled", irqEnabled)
        s.write("needIrq", needIrq)
        s.write("chrRegs", chrRegs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readInt("prgReg")
        irqCounter = s.readInt("irqCounter")
        irqScaler = s.readInt("irqScaler")
        irqEnabled = s.readBoolean("irqEnabled")
        needIrq = s.readBoolean("needIrq")
        s.readIntArray("chrRegs", chrRegs)
    }
}
