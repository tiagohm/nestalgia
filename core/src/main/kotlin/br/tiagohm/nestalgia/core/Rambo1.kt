package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.A12StateChange.*
import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_064

open class Rambo1(console: Console) : Mapper(console) {

    @Volatile private var irqEnabled = false
    @Volatile private var irqCycleMode = false
    @Volatile private var needReload = false
    @Volatile private var irqCounter = 0
    @Volatile private var irqReloadValue = 0
    @Volatile private var cpuClockCounter = 0
    private val a12Watcher = A12Watcher()
    private val registers = IntArray(16)
    @JvmField protected var currentRegister = 0
    @Volatile private var needIrqDelay = 0
    @Volatile private var forceClock = false

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override fun initialize() {
        selectPrgPage(3, -1)
    }

    override fun clock() {
        if (needIrqDelay > 0) {
            needIrqDelay--

            if (needIrqDelay == 0) {
                console.cpu.setIRQSource(EXTERNAL)
            }
        }

        if (irqCycleMode || forceClock) {
            cpuClockCounter = (cpuClockCounter + 1) and 0x03

            if (cpuClockCounter == 0) {
                clockIrqCounter(CPU_IRQ_DELAY)
                forceClock = false
            }
        }
    }

    private fun clockIrqCounter(delay: Int) {
        if (needReload) {
            // Fixes Hard Drivin'
            irqCounter = if (irqReloadValue <= 1) {
                irqReloadValue + 1
            } else {
                irqReloadValue + 2
            }

            needReload = false
        } else if (irqCounter == 0) {
            irqCounter = irqReloadValue + 1
        }

        irqCounter--

        if (irqCounter == 0 && irqEnabled) {
            needIrqDelay = delay
        }
    }

    private fun updateState() {
        if (currentRegister.bit6) {
            selectPrgPage(0, registers[15])
            selectPrgPage(1, registers[6])
            selectPrgPage(2, registers[7])
        } else {
            selectPrgPage(0, registers[6])
            selectPrgPage(1, registers[7])
            selectPrgPage(2, registers[15])
        }

        val a12Inversion = if (currentRegister.bit7) 0x04 else 0x00

        selectChrPage(0 xor a12Inversion, registers[0])
        selectChrPage(2 xor a12Inversion, registers[1])
        selectChrPage(4 xor a12Inversion, registers[2])
        selectChrPage(5 xor a12Inversion, registers[3])
        selectChrPage(6 xor a12Inversion, registers[4])
        selectChrPage(7 xor a12Inversion, registers[5])

        if (currentRegister.bit5) {
            selectChrPage(1 xor a12Inversion, registers[8])
            selectChrPage(3 xor a12Inversion, registers[9])
        } else {
            selectChrPage(1 xor a12Inversion, registers[0] + 1)
            selectChrPage(3 xor a12Inversion, registers[1] + 1)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE001) {
            0x8000 -> currentRegister = value
            0x8001 -> {
                registers[currentRegister and 0x0F] = value
                updateState()
            }
            0xA000 -> mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
            0xC000 -> irqReloadValue = value
            0xC001 -> {
                if (irqCycleMode && !value.bit0) {
                    // To be clear, after the write in the reg $C001, are needed more than
                    // four CPU clock cycles before the switch takes place, allowing another clock
                    // of irq running the reload.
                    //Fixes Skull & Crossbones
                    forceClock = true
                }

                irqCycleMode = value.bit0

                if (irqCycleMode) {
                    cpuClockCounter = 0
                }

                needReload = true
            }
            0xE000 -> {
                irqEnabled = false
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0xE001 -> irqEnabled = true
        }
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (!irqCycleMode) {
            if (a12Watcher.updateVRAMAddress(addr, console.ppu.frameCycle, 30) == RISE) {
                clockIrqCounter(PPU_IRQ_DELAY)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqEnabled", irqEnabled)
        s.write("irqCycleMode", irqCycleMode)
        s.write("needReload", needReload)
        s.write("irqCounter", irqCounter)
        s.write("irqReloadValue", irqReloadValue)
        s.write("cpuClockCounter", cpuClockCounter)
        s.write("a12Watcher", a12Watcher)
        s.write("registers", registers)
        s.write("currentRegister", currentRegister)
        s.write("needIrqDelay", needIrqDelay)
        s.write("forceClock", forceClock)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqEnabled = s.readBoolean("irqEnabled")
        irqCycleMode = s.readBoolean("irqCycleMode")
        needReload = s.readBoolean("needReload")
        irqCounter = s.readInt("irqCounter")
        irqReloadValue = s.readInt("irqReloadValue")
        cpuClockCounter = s.readInt("cpuClockCounter")
        s.readSnapshotable("a12Watcher", a12Watcher)
        s.readIntArray("registers", registers)
        currentRegister = s.readInt("currentRegister")
        needIrqDelay = s.readInt("needIrqDelay")
        forceClock = s.readBoolean("forceClock")
    }

    companion object {

        private const val PPU_IRQ_DELAY = 2
        private const val CPU_IRQ_DELAY = 1
    }
}
