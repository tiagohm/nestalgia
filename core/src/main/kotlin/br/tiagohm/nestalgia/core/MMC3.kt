package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_004

open class MMC3 : Mapper() {

    private var wramEnabled = false
    private var wramWriteProtected = false
    private var privateForceMmc3RevAIrqs = false
    private val a12Watcher = A12Watcher()

    protected var irqReloadValue = 0
    protected var irqCounter = 0
    protected var irqReload = false
    protected var irqEnabled = false
    protected var prgMode = 0
    protected var chrMode = 0
    protected val registers = IntArray(8)

    protected val state = IntArray(3)

    protected var currentRegister = 0
        private set

    protected open val forceMmc3RevAIrqs
        get() = privateForceMmc3RevAIrqs

    protected inline var state8000
        get() = state[0]
        set(value) {
            state[0] = value
        }

    protected inline var stateA000
        get() = state[1]
        set(value) {
            state[1] = value
        }

    protected inline var stateA001
        get() = state[2]
        set(value) {
            state[2] = value
        }

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val saveRamPageSize
        get() = if (info.subMapperId == 1) 0x200 else 0x2000

    override val saveRamSize
        get() = if (info.subMapperId == 1) 0x400 else 0x2000

    protected fun resetMMC3() {
        resetState()

        chrMode = powerOnByte() and 0x01
        prgMode = powerOnByte() and 0x01

        currentRegister = powerOnByte()

        resetRegisters()

        irqCounter = powerOnByte()
        irqReloadValue = powerOnByte()
        irqReload = powerOnByte().bit0
        irqEnabled = powerOnByte().bit0

        wramEnabled = powerOnByte().bit0
        wramWriteProtected = powerOnByte().bit0
    }

    private fun resetState() {
        for (i in state.indices) state[i] = powerOnByte()
    }

    private fun resetRegisters() {
        registers[0] = powerOnByte(0)
        registers[1] = powerOnByte(2)
        registers[2] = powerOnByte(4)
        registers[3] = powerOnByte(5)
        registers[4] = powerOnByte(6)
        registers[5] = powerOnByte(7)
        registers[6] = powerOnByte(0)
        registers[7] = powerOnByte(1)
    }

    protected open fun updateMirroring() {
        if (mirroringType != MirroringType.FOUR_SCREENS) {
            mirroringType = if (stateA000.bit0) MirroringType.HORIZONTAL else MirroringType.VERTICAL
        }
    }

    protected open fun updateChrMapping() {
        if (chrMode == 0) {
            selectChrPage(0, registers[0] and 0xFE)
            selectChrPage(1, registers[0] or 0x01)
            selectChrPage(2, registers[1] and 0xFE)
            selectChrPage(3, registers[1] or 0x01)

            selectChrPage(4, registers[2])
            selectChrPage(5, registers[3])
            selectChrPage(6, registers[4])
            selectChrPage(7, registers[5])
        } else if (chrMode == 1) {
            selectChrPage(0, registers[2])
            selectChrPage(1, registers[3])
            selectChrPage(2, registers[4])
            selectChrPage(3, registers[5])

            selectChrPage(4, registers[0] and 0xFE)
            selectChrPage(5, registers[0] or 0x01)
            selectChrPage(6, registers[1] and 0xFE)
            selectChrPage(7, registers[1] or 0x01)
        }
    }

    protected open fun updatePrgMapping() {
        if (prgMode == 0) {
            selectPrgPage(0, registers[6])
            selectPrgPage(1, registers[7])
            selectPrgPage(2, 0xFFFE)
            selectPrgPage(3, 0xFFFF)
        } else if (prgMode == 1) {
            selectPrgPage(0, 0xFFFE)
            selectPrgPage(1, registers[7])
            selectPrgPage(2, registers[6])
            selectPrgPage(3, 0xFFFF)
        }
    }

    protected val canWriteToWram
        get() = wramEnabled && !wramWriteProtected

    protected open fun updateState() {
        currentRegister = state8000 and 0x07
        chrMode = state8000 and 0x80 shr 7
        prgMode = state8000 and 0x40 shr 6

        if (info.mapperId == 4 && info.subMapperId == 1) {
            // MMC6
            val wramEnabled = state8000.bit5
            var firstBankAccess = (if (stateA001.bit4) 0x02 else 0x00) or if (stateA001.bit5) 0x01 else 0x00
            var lastBankAccess = (if (stateA001.bit6) 0x02 else 0x00) or if (stateA001.bit7) 0x01 else 0x00

            if (!wramEnabled) {
                firstBankAccess = 0x00
                lastBankAccess = 0x00
            }

            for (i in 0..3) {
                addCpuMemoryMapping(
                    0x7000 + i * 0x400,
                    0x71FF + i * 0x400,
                    0,
                    PrgMemoryType.SRAM,
                    MEMORY_ACCESS_TYPES[firstBankAccess],
                )
                addCpuMemoryMapping(
                    0x7200 + i * 0x400,
                    0x73FF + i * 0x400,
                    1,
                    PrgMemoryType.SRAM,
                    MEMORY_ACCESS_TYPES[lastBankAccess],
                )
            }
        } else {
            wramEnabled = stateA001.bit7
            wramWriteProtected = stateA001.bit6

            if (info.subMapperId == 0) {
                val access = if (wramEnabled) if (canWriteToWram) MemoryAccessType.READ_WRITE else MemoryAccessType.READ
                else MemoryAccessType.NO_ACCESS

                addCpuMemoryMapping(
                    0x6000,
                    0x7FFF,
                    0,
                    if (hasBattery) PrgMemoryType.SRAM else PrgMemoryType.WRAM,
                    access,
                )
            }
        }

        updatePrgMapping()
        updateChrMapping()
    }

    override fun initialize() {
        privateForceMmc3RevAIrqs = info.gameInfo?.chip == "MMC3A"

        resetMMC3()

        addCpuMemoryMapping(
            0x6000,
            0x7FFF,
            0,
            if (hasBattery) PrgMemoryType.SRAM else PrgMemoryType.WRAM,
        )

        updateState()
        updateMirroring()
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE001) {
            0x8000 -> {
                state8000 = value
                updateState()
            }
            0x8001 -> {
                if (currentRegister <= 1) {
                    // Writes to registers 0 and 1 always ignore bit 0
                    registers[currentRegister] = value and 0xFE
                } else {
                    registers[currentRegister] = value
                }

                updateState()
            }
            0xA000 -> {
                stateA000 = value
                updateMirroring()
            }
            0xA001 -> {
                stateA001 = value
                updateState()
            }
            0xC000 -> irqReloadValue = value
            0xC001 -> {
                irqCounter = 0
                irqReload = true
            }
            0xE000 -> {
                irqEnabled = false
                console.cpu.clearIRQSource(IRQSource.EXTERNAL)
            }
            0xE001 -> irqEnabled = true
        }
    }

    protected open fun triggerIrq() {
        console.cpu.setIRQSource(IRQSource.EXTERNAL)
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (a12Watcher.updateVRAMAddress(addr, console.ppu.frameCycle) == A12StateChange.RISE) {
            val count = irqCounter

            if (irqCounter == 0 || irqReload) {
                irqCounter = irqReloadValue
            } else {
                irqCounter--
            }

            if (forceMmc3RevAIrqs || console.settings.flag(EmulationFlag.MMC3_IRQ_ALT_BEHAVIOR)) {
                // MMC3 Revision A behavior
                if (((count > 0 && irqReloadValue > 0) || irqReload) && irqCounter == 0 && irqEnabled) {
                    triggerIrq()
                }
            } else if (irqCounter == 0 && irqEnabled) {
                triggerIrq()
            }

            irqReload = false
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("registers", registers)
        s.write("a12Watcher", a12Watcher)
        s.write("state", state)
        s.write("currentRegister", currentRegister)
        s.write("chrMode", chrMode)
        s.write("prgMode", prgMode)
        s.write("irqReloadValue", irqReloadValue)
        s.write("irqCounter", irqCounter)
        s.write("irqReload", irqReload)
        s.write("irqEnabled", irqEnabled)
        s.write("wramEnabled", wramEnabled)
        s.write("wramWriteProtected", wramWriteProtected)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("registers", registers) ?: resetRegisters()
        s.readSnapshotable("a12Watcher", a12Watcher) { a12Watcher.reset(false) }
        s.readIntArray("state", state) ?: resetState()
        currentRegister = s.readInt("currentRegister")
        chrMode = s.readInt("chrMode")
        prgMode = s.readInt("prgMode")
        irqReloadValue = s.readInt("irqReloadValue")
        irqCounter = s.readInt("irqCounter")
        irqReload = s.readBoolean("irqReload")
        irqEnabled = s.readBoolean("irqEnabled")
        wramEnabled = s.readBoolean("wramEnabled")
        wramWriteProtected = s.readBoolean("wramWriteProtected")
    }

    companion object {

        @JvmStatic protected val MEMORY_ACCESS_TYPES = arrayOf(
            MemoryAccessType.NO_ACCESS,
            MemoryAccessType.READ,
            MemoryAccessType.WRITE,
            MemoryAccessType.READ_WRITE,
        )
    }
}
