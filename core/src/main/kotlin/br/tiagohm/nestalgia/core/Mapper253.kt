package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.EXTERNAL
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_253

class Mapper253(console: Console) : Mapper(console) {

    private val chrLow = IntArray(8)
    private val chrHigh = IntArray(8)
    @Volatile private var forceChrRom = false
    @Volatile private var irqReloadValue = 0
    @Volatile private var irqCounter = 0
    @Volatile private var irqEnabled = false
    @Volatile private var irqScaler = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val chrRamSize = 0x800

    override val chrRamPageSize = 0x400

    override fun initialize() {
        selectPrgPage(2, -2)
        selectPrgPage(3, -1)
    }

    override fun clock() {
        if (irqEnabled) {
            irqScaler++

            if (irqScaler >= 114) {
                irqScaler = 0
                irqCounter = (irqCounter + 1) and 0xFF

                if (irqCounter == 0) {
                    irqCounter = irqReloadValue
                    console.cpu.setIRQSource(EXTERNAL)
                }
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr in 0xB000..0xE00C) {
            val slot = ((((addr and 0x08) or (addr shr 8)) shr 3) + 2) and 0x07
            val shift = addr and 0x04
            val low = ((chrLow[slot] and (0xF0 shr shift)) or (value shl shift)) and 0xFF

            chrLow[slot] = low

            if (slot == 0) {
                if (low == 0xC8) {
                    forceChrRom = false
                } else if (low == 0x88) {
                    forceChrRom = true
                }
            }

            if (shift != 0) {
                chrHigh[slot] = value shr 4
            }

            updateChr()
        } else {
            when (addr) {
                0x8010 -> selectPrgPage(0, value)
                0xA010 -> selectPrgPage(1, value)
                0x9400 -> mirroringType = when (value and 0x03) {
                    1 -> HORIZONTAL
                    2 -> SCREEN_A_ONLY
                    3 -> SCREEN_B_ONLY
                    else -> VERTICAL
                }
                0xF000 -> {
                    irqReloadValue = (irqReloadValue and 0xF0) or (value and 0x0F)
                    console.cpu.clearIRQSource(EXTERNAL)
                }
                0xF004 -> {
                    irqReloadValue = (irqReloadValue and 0x0F) or (value shl 4)
                    console.cpu.clearIRQSource(EXTERNAL)
                }
                0xF008 -> {
                    irqCounter = irqReloadValue
                    irqEnabled = value.bit1
                    irqScaler = 0
                    console.cpu.clearIRQSource(EXTERNAL)
                }
            }
        }
    }

    private fun updateChr() {
        repeat(8) {
            val page = chrLow[it] or (chrHigh[it] shl 8)

            if ((chrLow[it] == 4 || chrLow[it] == 5) && !forceChrRom) {
                selectChrPage(it, page and 0x01, ChrMemoryType.RAM)
            } else {
                selectChrPage(it, page)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chrLow", chrLow)
        s.write("chrHigh", chrHigh)
        s.write("forceChrRom", forceChrRom)
        s.write("irqReloadValue", irqReloadValue)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
        s.write("irqScaler", irqScaler)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("chrLow")?.copyInto(chrLow) ?: chrLow.fill(0)
        s.readIntArray("chrHigh")?.copyInto(chrHigh) ?: chrHigh.fill(0)
        forceChrRom = s.readBoolean("forceChrRom")
        irqReloadValue = s.readInt("irqReloadValue")
        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
        irqScaler = s.readInt("irqScaler")

        updateChr()
    }
}
