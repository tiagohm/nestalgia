package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/INES_Mapper_298

class Tf1201(console: Console) : Mapper(console) {

    private val chrRegs = IntArray(8)
    private val prgRegs = IntArray(2)
    @Volatile private var swapPrg = false
    @Volatile private var irqCounter = 0
    @Volatile private var irqReloadValue = 0
    @Volatile private var irqScaler = 0
    @Volatile private var irqEnabled = false

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override fun initialize() {
        updateChr()
        updatePrg()
    }

    override fun clock() {
        if (irqEnabled) {
            irqScaler -= 3

            if (irqScaler <= 0) {
                irqScaler += 341
                irqCounter = (irqCounter + 1) and 0xFF

                if (irqCounter == 0) {
                    console.cpu.setIRQSource(IRQSource.EXTERNAL)
                }
            }
        }
    }

    private fun updateChr() {
        repeat(8) {
            selectChrPage(it, chrRegs[it])
        }
    }

    private fun updatePrg() {
        if (swapPrg) {
            selectPrgPage(0, -2)
            selectPrgPage(2, prgRegs[0])
        } else {
            selectPrgPage(0, prgRegs[0])
            selectPrgPage(2, -2)
        }

        selectPrgPage(1, prgRegs[1])
        selectPrgPage(3, -1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val newAddr = (addr and 0xF003) or (addr and 0x0C shr 2)

        if (newAddr in 0xB000..0xE003) {
            val slot = (((newAddr shr 11) - 6) or (newAddr and 0x01)) and 0x07
            val shift = newAddr and 0x02 shl 1
            chrRegs[slot] = (chrRegs[slot] and (0xF0 shr shift)) or (value and 0x0F shl shift)
            updateChr()
        } else {
            when (newAddr and 0xF003) {
                0x8000 -> {
                    prgRegs[0] = value
                    updatePrg()
                }
                0xA000 -> {
                    prgRegs[1] = value
                    updatePrg()
                }
                0x9000 -> mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
                0x9001 -> {
                    swapPrg = value and 0x03 != 0
                    updatePrg()
                }
                0xF000 -> irqReloadValue = (irqReloadValue and 0xF0) or (value and 0x0F)
                0xF002 -> irqReloadValue = (irqReloadValue and 0x0F) or (value shl 4)
                0xF001 -> {
                    // VRC-like IRQs? This seems to make more sense than A12-based IRQs
                    // considering FCEUX/puNES both adjust the counter value based on when
                    // on the screen the IRQ is enabled.
                    // This still some glitches on the screen, but relatively minor ones.
                    irqEnabled = value.bit1

                    if (irqEnabled) {
                        irqScaler = 341
                        irqCounter = irqReloadValue
                    }

                    console.cpu.clearIRQSource(IRQSource.EXTERNAL)
                }
                0xF003 -> console.cpu.clearIRQSource(IRQSource.EXTERNAL)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chrRegs", chrRegs)
        s.write("prgRegs", prgRegs)
        s.write("swapPrg", swapPrg)
        s.write("irqCounter", irqCounter)
        s.write("irqReloadValue", irqReloadValue)
        s.write("irqScaler", irqScaler)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("chrRegs", chrRegs)
        s.readIntArray("prgRegs", prgRegs)
        swapPrg = s.readBoolean("swapPrg")
        irqCounter = s.readInt("irqCounter")
        irqReloadValue = s.readInt("irqReloadValue")
        irqScaler = s.readInt("irqScaler")
        irqEnabled = s.readBoolean("irqEnabled")
    }
}
