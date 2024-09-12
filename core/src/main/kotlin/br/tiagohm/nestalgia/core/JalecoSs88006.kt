package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_018

class JalecoSs88006(console: Console) : Mapper(console) {

    private val prgBanks = IntArray(3)
    private val chrBanks = IntArray(8)
    private val irqReloadValue = IntArray(4)
    private var irqCounter = 0
    private var irqCounterSize = 0
    private var irqEnabled = false

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override fun initialize() {
        selectPrgPage(3, -1)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updatePrgBank(bankNumber: Int, value: Int, updateUpperBits: Boolean) {
        if (updateUpperBits) {
            prgBanks[bankNumber] = prgBanks[bankNumber] and 0x0F or (value shl 4)
        } else {
            prgBanks[bankNumber] = prgBanks[bankNumber] and 0xF0 or value
        }

        selectPrgPage(bankNumber, prgBanks[bankNumber])
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateChrBank(bankNumber: Int, value: Int, updateUpperBits: Boolean) {
        if (updateUpperBits) {
            chrBanks[bankNumber] = chrBanks[bankNumber] and 0x0F or (value shl 4)
        } else {
            chrBanks[bankNumber] = chrBanks[bankNumber] and 0xF0 or value
        }

        selectChrPage(bankNumber, chrBanks[bankNumber])
    }

    override fun clock() {
        // Clock irq counter every memory read/write
        // (each cpu cycle either reads or writes memory).
        if (irqEnabled) {
            val irqMask = IRQ_MASK[irqCounterSize]
            val counter = (irqCounter and irqMask) - 1

            if (counter and 0xFFFF == 0) {
                console.cpu.setIRQSource(EXTERNAL)
            }

            irqCounter = irqCounter and irqMask.inv() or (counter and irqMask)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun reloadIrqCounter() {
        irqCounter = irqReloadValue[0] or (irqReloadValue[1] shl 4) or (irqReloadValue[2] shl 8) or (irqReloadValue[3] shl 12)
    }

    override fun writeRegister(addr: Int, value: Int) {
        val updateUpperBits = addr.bit0

        val newValue = value and 0x0F

        when (addr and 0xF003) {
            0x8000, 0x8001 -> updatePrgBank(0, newValue, updateUpperBits)
            0x8002, 0x8003 -> updatePrgBank(1, newValue, updateUpperBits)
            0x9000, 0x9001 -> updatePrgBank(2, newValue, updateUpperBits)
            0xA000, 0xA001 -> updateChrBank(0, newValue, updateUpperBits)
            0xA002, 0xA003 -> updateChrBank(1, newValue, updateUpperBits)
            0xB000, 0xB001 -> updateChrBank(2, newValue, updateUpperBits)
            0xB002, 0xB003 -> updateChrBank(3, newValue, updateUpperBits)
            0xC000, 0xC001 -> updateChrBank(4, newValue, updateUpperBits)
            0xC002, 0xC003 -> updateChrBank(5, newValue, updateUpperBits)
            0xD000, 0xD001 -> updateChrBank(6, newValue, updateUpperBits)
            0xD002, 0xD003 -> updateChrBank(7, newValue, updateUpperBits)
            0xE000, 0xE001, 0xE002, 0xE003 -> irqReloadValue[addr and 0x03] = newValue
            0xF000 -> {
                console.cpu.clearIRQSource(EXTERNAL)
                reloadIrqCounter()
            }
            0xF001 -> {
                console.cpu.clearIRQSource(EXTERNAL)

                irqEnabled = newValue.bit0

                irqCounterSize =
                    // 4-bit counter.
                    if (newValue.bit3) 3
                    // 12-bit counter.
                    else if (newValue.bit2) 2
                    // 16-bit counter.
                    else if (newValue.bit1) 1
                    // 8-bit counter.
                    else 0
            }
            0xF002 -> when (newValue) {
                0 -> mirroringType = HORIZONTAL
                1 -> mirroringType = VERTICAL
                2 -> mirroringType = SCREEN_A_ONLY
                3 -> mirroringType = SCREEN_B_ONLY
            }
            // TODO: Expansion audio, not supported yet.
            0xF003 -> Unit
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgBanks", prgBanks)
        s.write("chrBanks", chrBanks)
        s.write("irqReloadValue", irqReloadValue)
        s.write("irqCounter", irqCounter)
        s.write("irqCounterSize", irqCounterSize)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("prgBanks", prgBanks)
        s.readIntArray("chrBanks", chrBanks)
        s.readIntArray("irqReloadValue", irqReloadValue)
        irqCounter = s.readInt("irqCounter")
        irqCounterSize = s.readInt("irqCounterSize")
        irqEnabled = s.readBoolean("irqEnabled")
    }

    companion object {

        private val IRQ_MASK = intArrayOf(0xFFFF, 0x0FFF, 0x00FF, 0x000F)
    }
}
