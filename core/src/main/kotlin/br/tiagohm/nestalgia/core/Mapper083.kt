package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INESMapper083

class Mapper083(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val dipSwitchCount = 2

    override val allowRegisterRead = true

    private val regs = IntArray(11)
    private val exRegs = IntArray(4)
    private var is2kBank = false
    private var isNot2kBank = false
    private var mode = 0
    private var bank = 0
    private var irqCounter = 0
    private var irqEnabled = false

    override fun initialize() {
        addRegisterRange(0x5000, 0x5000, READ)
        addRegisterRange(0x5100, 0x5103, READ_WRITE)
        removeRegisterRange(0x8000, 0xFFFF, READ)

        updateState()
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter--

            if (irqCounter <= 0) {
                irqEnabled = false
                irqCounter = 0xFFFF
                console.cpu.setIRQSource(EXTERNAL)
            }
        }
    }

    private fun updateState() {
        when (mode and 0x03) {
            0 -> mirroringType = VERTICAL
            1 -> mirroringType = HORIZONTAL
            2 -> mirroringType = SCREEN_A_ONLY
            3 -> mirroringType = SCREEN_B_ONLY
        }

        if (is2kBank && !isNot2kBank) {
            selectChrPage2x(0, regs[0] shl 1)
            selectChrPage2x(1, regs[1] shl 1)
            selectChrPage2x(2, regs[6] shl 1)
            selectChrPage2x(3, regs[7] shl 1)
        } else {
            repeat(8) {
                selectChrPage(it, regs[it] or (bank and 0x30 shl 4))
            }
        }

        if (mode.bit6) {
            selectPrgPage2x(0, bank and 0x3F shl 1)
            selectPrgPage2x(1, bank and 0x30 or 0x0F shl 1)
        } else {
            selectPrgPage(0, regs[8])
            selectPrgPage(1, regs[9])
            selectPrgPage(2, regs[10])
            selectPrgPage(3, -1)
        }
    }

    override fun readRegister(addr: Int): Int {
        return if (addr == 0x5000) {
            console.memoryManager.openBus(0xFC) or dipSwitches
        } else {
            exRegs[addr and 0x03]
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            exRegs[addr and 0x03] = value
        } else if (addr in 0x8300..0x8302) {
            mode = mode and 0xBF
            regs[addr - 0x8300 + 8] = value
            updateState()
        } else if (addr in 0x8310..0x8317) {
            regs[addr - 0x8310] = value

            if (addr in 0x8312..0x8315) {
                isNot2kBank = true
            }

            updateState()
        } else {
            when (addr) {
                0x8000 -> {
                    is2kBank = true
                    bank = value
                    mode = mode or 0x40
                    updateState()
                }
                0xB000, 0xB0FF, 0xB1FF -> {
                    // Dragon Ball Z Party [p1] BMC.
                    bank = value
                    mode = mode or 0x40
                    updateState()
                }
                0x8100 -> {
                    mode = value or (mode and 0x40)
                    updateState()
                }
                0x8200 -> {
                    irqCounter = irqCounter and 0xFF00 or value
                    console.cpu.clearIRQSource(EXTERNAL)
                }
                0x8201 -> {
                    irqEnabled = mode.bit7
                    irqCounter = irqCounter and 0xFF or (value shl 8)
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
        s.write("exRegs", exRegs)
        s.write("is2kBank", is2kBank)
        s.write("isNot2kBank", isNot2kBank)
        s.write("mode", mode)
        s.write("bank", bank)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
        s.readIntArray("exRegs", exRegs)

        is2kBank = s.readBoolean("is2kBank")
        isNot2kBank = s.readBoolean("isNot2kBank")
        mode = s.readInt("mode")
        bank = s.readInt("bank")
        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
    }
}
