package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.EmulationFlag.MMC3_IRQ_ALT_BEHAVIOR
import br.tiagohm.nestalgia.core.IRQSource.EXTERNAL
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.SRAM
import br.tiagohm.nestalgia.core.PrgMemoryType.WRAM

// https://wiki.nesdev.com/w/index.php/INES_Mapper_004

open class MMC3(console: Console) : Mapper(console) {

    protected data class State(
        @JvmField var reg8000: Int = 0,
        @JvmField var regA000: Int = 0,
        @JvmField var regA001: Int = 0,
    ) : Snapshotable {

        override fun saveState(s: Snapshot) {
            s.write("reg8000", reg8000)
            s.write("regA000", regA000)
            s.write("regA001", regA001)
        }

        override fun restoreState(s: Snapshot) {
            reg8000 = s.readInt("reg8000")
            regA000 = s.readInt("regA000")
            regA001 = s.readInt("regA001")
        }
    }

    @Volatile private var wramEnabled = false
    @Volatile private var wramWriteProtected = false
    @Volatile private var mForceMmc3RevAIrqs = false
    private val a12Watcher = A12RisingEdgeWatcher(console)

    @JvmField @Volatile protected var irqReloadValue = 0
    @JvmField @Volatile protected var irqCounter = 0
    @JvmField @Volatile protected var irqReload = false
    @JvmField @Volatile protected var irqEnabled = false
    @JvmField @Volatile protected var prgMode = false
    @JvmField @Volatile protected var chrMode = false
    protected val registers = IntArray(8)

    protected val state = State()

    @JvmField @Volatile protected var currentRegister = 0

    protected open val forceMmc3RevAIrqs
        get() = mForceMmc3RevAIrqs

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val saveRamPageSize
        get() = if (info.subMapperId == 1) 0x200 else 0x2000

    override val saveRamSize
        get() = if (info.subMapperId == 1) 0x400 else 0x2000

    protected fun resetMMC3() {
        resetState()

        chrMode = powerOnByte().bit0
        prgMode = powerOnByte().bit0

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
        state.reg8000 = powerOnByte()
        state.regA000 = powerOnByte()
        state.regA001 = powerOnByte()
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
        if (mirroringType != FOUR_SCREENS) {
            mirroringType = if (state.regA000.bit0) HORIZONTAL else VERTICAL
        }
    }

    protected open fun updateChrMapping() {
        if (chrMode) {
            selectChrPage(0, registers[2])
            selectChrPage(1, registers[3])
            selectChrPage(2, registers[4])
            selectChrPage(3, registers[5])

            selectChrPage(4, registers[0] and 0xFE)
            selectChrPage(5, registers[0] or 0x01)
            selectChrPage(6, registers[1] and 0xFE)
            selectChrPage(7, registers[1] or 0x01)
        } else {
            selectChrPage(0, registers[0] and 0xFE)
            selectChrPage(1, registers[0] or 0x01)
            selectChrPage(2, registers[1] and 0xFE)
            selectChrPage(3, registers[1] or 0x01)

            selectChrPage(4, registers[2])
            selectChrPage(5, registers[3])
            selectChrPage(6, registers[4])
            selectChrPage(7, registers[5])
        }
    }

    protected open fun updatePrgMapping() {
        if (prgMode) {
            selectPrgPage(0, 0xFFFE)
            selectPrgPage(1, registers[7])
            selectPrgPage(2, registers[6])
            selectPrgPage(3, 0xFFFF)
        } else {
            selectPrgPage(0, registers[6])
            selectPrgPage(1, registers[7])
            selectPrgPage(2, 0xFFFE)
            selectPrgPage(3, 0xFFFF)
        }
    }

    protected val canWriteToWram
        get() = wramEnabled && !wramWriteProtected

    protected open fun updateState() {
        currentRegister = state.reg8000 and 0x07
        chrMode = state.reg8000.bit7
        prgMode = state.reg8000.bit6

        if (info.mapperId == 4 && info.subMapperId == 1) {
            // MMC6
            val wramEnabled = state.reg8000.bit5
            var firstBankAccess = (if (state.regA001.bit4) 0x02 else 0x00) or if (state.regA001.bit5) 0x01 else 0x00
            var lastBankAccess = (if (state.regA001.bit6) 0x02 else 0x00) or if (state.regA001.bit7) 0x01 else 0x00

            if (!wramEnabled) {
                firstBankAccess = 0x00
                lastBankAccess = 0x00
            }

            repeat(4) {
                val k = it * 0x400
                addCpuMemoryMapping(0x7000 + k, 0x71FF + k, 0, SRAM, MEMORY_ACCESS_TYPES[firstBankAccess])
                addCpuMemoryMapping(0x7200 + k, 0x73FF + k, 1, SRAM, MEMORY_ACCESS_TYPES[lastBankAccess])
            }
        } else {
            wramEnabled = state.regA001.bit7
            wramWriteProtected = state.regA001.bit6

            if (info.subMapperId == 0) {
                val access = if (wramEnabled) if (canWriteToWram) READ_WRITE else READ
                else NO_ACCESS

                if (hasBattery && mSaveRamSize > 0 ||
                    !hasBattery && mWorkRamSize > 0
                ) {
                    addCpuMemoryMapping(0x6000, 0x7FFF, 0, if (hasBattery) SRAM else WRAM, access)
                } else {
                    removeCpuMemoryMapping(0x6000, 0x7FFF)
                }
            }
        }

        updatePrgMapping()
        updateChrMapping()
    }

    override fun initialize() {
        mForceMmc3RevAIrqs = info.gameInfo?.chip?.startsWith("MMC3A") == true

        resetMMC3()

        addCpuMemoryMapping(0x6000, 0x7FFF, 0, if (hasBattery) SRAM else WRAM)

        updateState()
        updateMirroring()
    }

    override fun writeRegister(addr: Int, value: Int) {
        mmc3WriteRegister(addr, value)
    }

    protected fun mmc3WriteRegister(addr: Int, value: Int) {
        when (addr and 0xE001) {
            0x8000 -> {
                state.reg8000 = value
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
                state.regA000 = value
                updateMirroring()
            }
            0xA001 -> {
                state.regA001 = value
                updateState()
            }
            0xC000 -> irqReloadValue = value
            0xC001 -> {
                irqCounter = 0
                irqReload = true
            }
            0xE000 -> {
                irqEnabled = false
                console.cpu.clearIRQSource(EXTERNAL)
            }
            0xE001 -> irqEnabled = true
        }
    }

    protected open fun triggerIrq() {
        console.cpu.setIRQSource(EXTERNAL)
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (a12Watcher.isRisingEdge(addr)) {
            val count = irqCounter

            if (irqCounter == 0 || irqReload) {
                irqCounter = irqReloadValue
            } else {
                irqCounter--
            }

            if (forceMmc3RevAIrqs || console.settings.flag(MMC3_IRQ_ALT_BEHAVIOR)) {
                // MMC3 Revision A behavior.
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
        s.readSnapshotable("a12Watcher", a12Watcher, a12Watcher::reset)
        s.readSnapshotable("state", state, ::resetState)
        currentRegister = s.readInt("currentRegister")
        chrMode = s.readBoolean("chrMode")
        prgMode = s.readBoolean("prgMode")
        irqReloadValue = s.readInt("irqReloadValue")
        irqCounter = s.readInt("irqCounter")
        irqReload = s.readBoolean("irqReload")
        irqEnabled = s.readBoolean("irqEnabled")
        wramEnabled = s.readBoolean("wramEnabled")
        wramWriteProtected = s.readBoolean("wramWriteProtected")
    }

    companion object {

        internal val MEMORY_ACCESS_TYPES = arrayOf(NO_ACCESS, READ, WRITE, READ_WRITE)
    }
}
