package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_264

class Yoko(console: Console) : Mapper(console) {

    private val regs = IntArray(7)
    private val exRegs = IntArray(4)
    @Volatile private var mode = 0
    @Volatile private var bank = 0
    @Volatile private var irqCounter = 0
    @Volatile private var irqEnabled = false

    override val dipSwitchCount = 2

    override val registerStartAddress = 0x5000

    override val registerEndAddress = 0x5FFF

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x800

    override val allowRegisterRead = true

    override fun initialize() {
        removeRegisterRange(0x5000, 0x53FF, MemoryAccessType.WRITE)
        addRegisterRange(0x8000, 0xFFFF, MemoryAccessType.WRITE)

        updateState()
    }

    override fun reset(softReset: Boolean) {
        if (softReset) {
            mode = 0
            bank = 0
        }
    }

    override fun clock() {
        if (irqEnabled) {
            irqCounter--

            if (irqCounter == 0) {
                irqEnabled = false
                irqCounter = 0xFFFF

                console.cpu.setIRQSource(IRQSource.EXTERNAL)
            }
        }
    }

    private fun updateState() {
        mirroringType = if (mode.bit0) HORIZONTAL else VERTICAL

        selectChrPage(0, regs[3])
        selectChrPage(1, regs[4])
        selectChrPage(2, regs[5])
        selectChrPage(3, regs[6])

        if (mode.bit4) {
            val outer = bank and 0x08 shl 1
            selectPrgPage(0, outer or (regs[0] and 0x0F))
            selectPrgPage(1, outer or (regs[1] and 0x0F))
            selectPrgPage(2, outer or (regs[2] and 0x0F))
            selectPrgPage(3, outer or 0x0F)
        } else if (mode.bit3) {
            selectPrgPage4x(0, bank and 0xFE shl 1)
        } else {
            selectPrgPage2x(0, bank shl 1)
            selectPrgPage2x(1, -2)
        }
    }

    override fun readRegister(addr: Int): Int {
        return if (addr <= 0x53FF) {
            console.memoryManager.openBus(0xFC) or dipSwitches
        } else {
            exRegs[addr and 0x03]
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            exRegs[addr and 0x03] = value
        } else {
            when (addr and 0x8C17) {
                0x8000 -> {
                    bank = value
                    updateState()
                }
                0x8400 -> {
                    mode = value
                    updateState()
                }
                0x8800 -> {
                    irqCounter = (irqCounter and 0xFF00) or value
                    console.cpu.clearIRQSource(IRQSource.EXTERNAL)
                }
                0x8801 -> {
                    irqEnabled = mode.bit7
                    irqCounter = (irqCounter and 0xFF) or (value shl 8)
                }
                0x8c00 -> {
                    regs[0] = value
                    updateState()
                }
                0x8c01 -> {
                    regs[1] = value
                    updateState()
                }
                0x8c02 -> {
                    regs[2] = value
                    updateState()
                }
                0x8c10 -> {
                    regs[3] = value
                    updateState()
                }
                0x8c11 -> {
                    regs[4] = value
                    updateState()
                }
                0x8c16 -> {
                    regs[5] = value
                    updateState()
                }
                0x8c17 -> {
                    regs[6] = value
                    updateState()
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
        s.write("exRegs", exRegs)
        s.write("mode", mode)
        s.write("bank", bank)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
        s.readIntArray("exRegs", exRegs)
        mode = s.readInt("mode")
        bank = s.readInt("bank")
        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
    }
}
