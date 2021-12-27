package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_004

@Suppress("NOTHING_TO_INLINE")
open class MMC3 : Mapper() {

    private var wramEnabled = false
    private var wramWriteProtected = false
    private var privateForceMmc3RevAIrqs = false
    private val a12Watcher = A12Watcher()

    protected var irqReloadValue: UByte = 0U
    protected var irqCounter: UByte = 0U
    protected var irqReload = false
    protected var irqEnabled = false
    protected var prgMode: UByte = 0U
    protected var chrMode: UByte = 0U
    protected val registers = UByteArray(8)

    protected val state = UByteArray(3)

    protected var currentRegister: UByte = 0U
        private set

    protected open val forceMmc3RevAIrqs: Boolean
        get() = privateForceMmc3RevAIrqs

    protected inline var state8000: UByte
        get() = state[0]
        set(value) {
            state[0] = value
        }

    protected inline var stateA000: UByte
        get() = state[1]
        set(value) {
            state[1] = value
        }

    protected inline var stateA001: UByte
        get() = state[2]
        set(value) {
            state[2] = value
        }

    override val prgPageSize = 0x2000U

    override val chrPageSize = 0x400U

    override val saveRamPageSize: UInt
        get() = if (info.subMapperId == 1) 0x200U else 0x2000U

    override val saveRamSize: UInt
        get() = if (info.subMapperId == 1) 0x400U else 0x2000U

    protected fun resetMMC3() {
        resetState()

        chrMode = getPowerOnByte() and 0x01U
        prgMode = getPowerOnByte() and 0x01U

        currentRegister = getPowerOnByte()

        resetRegisters()

        irqCounter = getPowerOnByte()
        irqReloadValue = getPowerOnByte()
        irqReload = getPowerOnByte().bit0
        irqEnabled = getPowerOnByte().bit0

        wramEnabled = getPowerOnByte().bit0
        wramWriteProtected = getPowerOnByte().bit0
    }

    private fun resetState() {
        for (i in state.indices) state[i] = getPowerOnByte()
    }

    private fun resetRegisters() {
        registers[0] = getPowerOnByte(0U)
        registers[1] = getPowerOnByte(2U)
        registers[2] = getPowerOnByte(4U)
        registers[3] = getPowerOnByte(5U)
        registers[4] = getPowerOnByte(6U)
        registers[5] = getPowerOnByte(7U)
        registers[6] = getPowerOnByte(0U)
        registers[7] = getPowerOnByte(1U)
    }

    protected open fun updateMirroring() {
        if (mirroringType != MirroringType.FOUR_SCREENS) {
            mirroringType = if (stateA000.bit0) MirroringType.HORIZONTAL else MirroringType.VERTICAL
        }
    }

    protected open fun updateChrMapping() {
        if (chrMode.isZero) {
            selectChrPage(0U, (registers[0] and 0xFEU).toUShort())
            selectChrPage(1U, (registers[0] or 0x01U).toUShort())
            selectChrPage(2U, (registers[1] and 0xFEU).toUShort())
            selectChrPage(3U, (registers[1] or 0x01U).toUShort())

            selectChrPage(4U, registers[2].toUShort())
            selectChrPage(5U, registers[3].toUShort())
            selectChrPage(6U, registers[4].toUShort())
            selectChrPage(7U, registers[5].toUShort())
        } else if (chrMode.isOne) {
            selectChrPage(0U, registers[2].toUShort())
            selectChrPage(1U, registers[3].toUShort())
            selectChrPage(2U, registers[4].toUShort())
            selectChrPage(3U, registers[5].toUShort())

            selectChrPage(4U, (registers[0] and 0xFEU).toUShort())
            selectChrPage(5U, (registers[0] or 0x01U).toUShort())
            selectChrPage(6U, (registers[1] and 0xFEU).toUShort())
            selectChrPage(7U, (registers[1] or 0x01U).toUShort())
        }
    }

    protected open fun updatePrgMapping() {
        if (prgMode.isZero) {
            selectPrgPage(0U, registers[6].toUShort())
            selectPrgPage(1U, registers[7].toUShort())
            selectPrgPage(2U, 0xFFFEU)
            selectPrgPage(3U, 0xFFFFU)
        } else if (prgMode.isOne) {
            selectPrgPage(0U, 0xFFFEU)
            selectPrgPage(1U, registers[7].toUShort())
            selectPrgPage(2U, registers[6].toUShort())
            selectPrgPage(3U, 0xFFFFU)
        }
    }

    protected val canWriteToWram: Boolean
        get() = wramEnabled && !wramWriteProtected

    protected open fun updateState() {
        currentRegister = state8000 and 0x07U
        chrMode = (state8000 and 0x80U) shr 7
        prgMode = (state8000 and 0x40U) shr 6

        if (info.mapperId == 4 && info.subMapperId == 1) {
            // MMC6
            val wramEnabled = state8000.bit5
            var firstBankAccess = (if (stateA001.bit4) 0x02 else 0x00) or if (stateA001.bit5) 0x01 else 0x00
            var lastBankAccess = (if (stateA001.bit6) 0x02 else 0x00) or if (stateA001.bit7) 0x01 else 0x00

            if (!wramEnabled) {
                firstBankAccess = 0x00
                lastBankAccess = 0x00
            }

            for (i in 0U..3U) {
                setCpuMemoryMapping(
                    (0x7000U + i * 0x400U).toUShort(),
                    (0x71FFU + i * 0x400U).toUShort(),
                    0.toShort(),
                    PrgMemoryType.SRAM,
                    MEMORY_ACCESS_TYPES[firstBankAccess]
                )
                setCpuMemoryMapping(
                    (0x7200U + i * 0x400U).toUShort(),
                    (0x73FFU + i * 0x400U).toUShort(),
                    1.toShort(),
                    PrgMemoryType.SRAM,
                    MEMORY_ACCESS_TYPES[lastBankAccess]
                )
            }
        } else {
            wramEnabled = stateA001.bit7
            wramWriteProtected = stateA001.bit6

            if (info.subMapperId == 0) {
                val access = if (wramEnabled) if (canWriteToWram) MemoryAccessType.READ_WRITE else MemoryAccessType.READ
                else MemoryAccessType.NO_ACCESS

                setCpuMemoryMapping(
                    0x6000U,
                    0x7FFFU,
                    0.toShort(),
                    if (hasBattery) PrgMemoryType.SRAM else PrgMemoryType.WRAM,
                    access
                )
            }
        }

        updatePrgMapping()
        updateChrMapping()
    }

    override fun init() {
        privateForceMmc3RevAIrqs = info.gameInfo?.chip == "MMC3A"

        resetMMC3()

        setCpuMemoryMapping(
            0x6000U,
            0x7FFFU,
            0.toShort(),
            if (hasBattery) PrgMemoryType.SRAM else PrgMemoryType.WRAM
        )

        updateState()
        updateMirroring()
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        when (addr.toInt() and 0xE001) {
            0x8000 -> {
                state8000 = value
                updateState()
            }
            0x8001 -> {
                if (currentRegister <= 1U) {
                    // Writes to registers 0 and 1 always ignore bit 0
                    registers[currentRegister.toInt()] = value and 0xFEU
                } else {
                    registers[currentRegister.toInt()] = value
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
                irqCounter = 0U
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

    override fun notifyVRAMAddressChange(addr: UShort) {
        if (a12Watcher.updateVRAMAddress(addr, console.ppu.frameCycle) == A12StateChange.RISE) {
            val count = irqCounter

            if (irqCounter.isZero || irqReload) {
                irqCounter = irqReloadValue
            } else {
                irqCounter--
            }

            if (forceMmc3RevAIrqs || console.settings.checkFlag(EmulationFlag.MMC3_IRQ_ALT_BEHAVIOR)) {
                // MMC3 Revision A behavior
                if (((count > 0U && irqReloadValue > 0U) || irqReload) && irqCounter.isZero && irqEnabled) {
                    triggerIrq()
                }
            } else if (irqCounter.isZero && irqEnabled) {
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

        s.readUByteArray("registers")?.copyInto(registers) ?: resetRegisters()
        s.readSnapshot("a12Watcher")?.let { a12Watcher.restoreState(it) } ?: a12Watcher.reset(false)
        s.readUByteArray("state")?.copyInto(state) ?: resetState()
        currentRegister = s.readUByte("currentRegister") ?: 0U
        chrMode = s.readUByte("chrMode") ?: 0U
        prgMode = s.readUByte("prgMode") ?: 0U
        irqReloadValue = s.readUByte("irqReloadValue") ?: 0U
        irqCounter = s.readUByte("irqCounter") ?: 0U
        irqReload = s.readBoolean("irqReload") ?: false
        irqEnabled = s.readBoolean("irqEnabled") ?: false
        wramEnabled = s.readBoolean("wramEnabled") ?: false
        wramWriteProtected = s.readBoolean("wramWriteProtected") ?: false
    }

    companion object {

        protected val MEMORY_ACCESS_TYPES = arrayOf(
            MemoryAccessType.NO_ACCESS,
            MemoryAccessType.READ,
            MemoryAccessType.WRITE,
            MemoryAccessType.READ_WRITE
        )
    }
}