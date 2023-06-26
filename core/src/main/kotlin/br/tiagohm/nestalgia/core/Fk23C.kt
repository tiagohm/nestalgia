package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.A12StateChange.*
import br.tiagohm.nestalgia.core.ChrMemoryType.*
import br.tiagohm.nestalgia.core.ChrMemoryType.ROM
import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_176

class Fk23C(console: Console) : Mapper(console) {

    private var prgBankingMode = 0
    private var outerChrBankSize = 0
    private var selectChrRam = false
    private var mmc3ChrMode = true
    private var cnromChrMode = false
    private var prgBaseBits = 0
    private var chrBaseBits = 0
    private var extendedMmc3Mode = false
    private var wramBankSelect = 0
    private var ramInFirstChrBank = false
    private var allowSingleScreenMirroring = false
    private var fk23RegistersEnabled = false
    private var wramConfigEnabled = false

    private var wramEnabled = false
    private var wramWriteProtected = false

    private var invertPrgA14 = false
    private var invertChrA12 = false

    private var currentRegister = 0

    private var irqReloadValue = 0
    private var irqCounter = 0
    private var irqReload = false
    private var irqEnabled = false

    private var mirroringReg = 0

    private var cnromChrReg = 0

    private var irqDelay = 0

    private val mmc3Registers = intArrayOf(0, 2, 4, 5, 6, 7, 0, 1, 0xFE, 0xFF, 0xFF, 0xFF)
    private val a12Watcher = A12Watcher()

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override val chrRamSize = 0x40000

    override val chrRamPageSize = 0x400

    override val workRamSize = 0x8000

    override val workRamPageSize = 0x2000

    override fun initialize() {
        // Subtype 1, 1024 KiB PRG-ROM, 1024 KiB CHR-ROM: boot in second 512 KiB of PRG-ROM.
        prgBaseBits = if (mPrgSize == 1024 * 1024 && mPrgSize == mChrRomSize) 0x20 else 0

        addRegisterRange(0x5000, 0x5FFF, WRITE)
        updateState()
    }

    override fun reset(softReset: Boolean) {
        if (softReset) {
            if (wramConfigEnabled && selectChrRam && hasBattery) {
                prgBaseBits = 0
                updateState()
            }
        }
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        val useChrRam = !hasChrRom || selectChrRam && mChrRamSize > 0 || wramConfigEnabled && ramInFirstChrBank && page <= 7
        super.selectChrPage(slot, page, if (useChrRam) RAM else ROM)
    }

    private fun updatePrg() {
        when (prgBankingMode) {
            0, 1, 2 -> if (extendedMmc3Mode) {
                val swap = if (invertPrgA14) 2 else 0
                val outer = prgBaseBits shl 1

                selectPrgPage(0 xor swap, mmc3Registers[6] or outer)
                selectPrgPage(1, mmc3Registers[7] or outer)
                selectPrgPage(2 xor swap, mmc3Registers[8] or outer)
                selectPrgPage(3, mmc3Registers[9] or outer)
            } else {
                val swap = if (invertPrgA14) 2 else 0
                val innerMask = 0x3F shr prgBankingMode
                val outer = prgBaseBits shl 1 and innerMask.inv()

                selectPrgPage(0 xor swap, mmc3Registers[6] and innerMask or outer)
                selectPrgPage(1, mmc3Registers[7] and innerMask or outer)
                selectPrgPage(2 xor swap, 0xFE and innerMask or outer)
                selectPrgPage(3, 0xFF and innerMask or outer)
            }
            3 -> {
                selectPrgPage2x(0, prgBaseBits shl 1)
                selectPrgPage2x(1, prgBaseBits shl 1)
            }
            4 -> selectPrgPage4x(0, prgBaseBits and 0xFFE shl 1)
        }
    }

    private fun updateChr() {
        if (!mmc3ChrMode) {
            val innerMask = if (cnromChrMode) (if (outerChrBankSize > 0) 1 else 3) else 0
            val page = cnromChrReg and innerMask or chrBaseBits shl 3

            repeat(8) {
                selectChrPage(it, page + it)
            }
        } else {
            val swap = if (invertChrA12) 0x04 else 0

            if (extendedMmc3Mode) {
                val outer = chrBaseBits shl 3
                selectChrPage(0 xor swap, mmc3Registers[0] or outer)
                selectChrPage(1 xor swap, mmc3Registers[10] or outer)
                selectChrPage(2 xor swap, mmc3Registers[1] or outer)
                selectChrPage(3 xor swap, mmc3Registers[11] or outer)
                selectChrPage(4 xor swap, mmc3Registers[2] or outer)
                selectChrPage(5 xor swap, mmc3Registers[3] or outer)
                selectChrPage(6 xor swap, mmc3Registers[4] or outer)
                selectChrPage(7 xor swap, mmc3Registers[5] or outer)
            } else {
                val innerMask = if (outerChrBankSize > 0) 0x7F else 0xFF
                val outer = chrBaseBits shl 3 and innerMask.inv()

                selectChrPage(0 xor swap, mmc3Registers[0] and 0xFE and innerMask or outer)
                selectChrPage(1 xor swap, mmc3Registers[0] or 0x01 and innerMask or outer)
                selectChrPage(2 xor swap, mmc3Registers[1] and 0xFE and innerMask or outer)
                selectChrPage(3 xor swap, mmc3Registers[1] or 0x01 and innerMask or outer)
                selectChrPage(4 xor swap, mmc3Registers[2] and innerMask or outer)
                selectChrPage(5 xor swap, mmc3Registers[3] and innerMask or outer)
                selectChrPage(6 xor swap, mmc3Registers[4] and innerMask or outer)
                selectChrPage(7 xor swap, mmc3Registers[5] and innerMask or outer)
            }
        }
    }

    private fun updateState() {
        when (mirroringReg and if (allowSingleScreenMirroring) 0x03 else 0x01) {
            0 -> mirroringType = VERTICAL
            1 -> mirroringType = HORIZONTAL
            2 -> mirroringType = SCREEN_A_ONLY
            3 -> mirroringType = SCREEN_B_ONLY
        }

        updatePrg()
        updateChr()

        if (wramConfigEnabled) {
            val nextBank = wramBankSelect + 1 and 0x03

            addCpuMemoryMapping(0x4000, 0x5FFF, nextBank, if (hasBattery) SRAM else WRAM, READ_WRITE)
            addCpuMemoryMapping(0x6000, 0x7FFF, wramBankSelect, if (hasBattery) SRAM else WRAM, READ_WRITE)
        } else {
            if (wramEnabled) {
                addCpuMemoryMapping(0x6000, 0x7FFF, 0, WRAM, if (wramWriteProtected) READ else READ_WRITE)
            } else {
                removeCpuMemoryMapping(0x6000, 0x7FFF)
            }

            removeCpuMemoryMapping(0x4000, 0x5FFF)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (fk23RegistersEnabled || !wramConfigEnabled) {
                if (addr and 0x5010 != 0x5010) {
                    // Not a register.
                    return
                }

                when (addr and 0x03) {
                    0 -> {
                        prgBankingMode = value and 0x07
                        outerChrBankSize = value and 0x10 shr 4
                        selectChrRam = value.bit5
                        mmc3ChrMode = !value.bit6
                        prgBaseBits = prgBaseBits and 0x180.inv() or (value and 0x80 shl 1) or (value and 0x08 shl 4)
                    }
                    1 -> prgBaseBits = prgBaseBits and 0x7F.inv() or (value and 0x7F)
                    2 -> {
                        prgBaseBits = prgBaseBits and 0x200.inv() or (value and 0x40 shl 3)
                        chrBaseBits = value
                        cnromChrReg = 0
                    }
                    3 -> {
                        extendedMmc3Mode = value.bit1
                        cnromChrMode = value and 0x44 != 0
                    }
                }

                updateState()
            } else {
                // FK23C Registers disabled, $5000-$5FFF maps to the second 4 KiB
                // of the 8 KiB WRAM bank 2.
                writePrgRam(addr, value)
            }
        } else {
            if (cnromChrMode && (addr <= 0x9FFF || addr >= 0xC000)) {
                cnromChrReg = value and 0x03
                updateState()
            }

            when (addr and 0xE001) {
                0x8000 -> {
                    // Subtype 2, 16384 KiB PRG-ROM, no CHR-ROM: Like Subtype 0, but MMC3 registers $46 and $47 swapped.
                    val newValue = if (mPrgSize == 16384 * 1024 && (value == 0x46 || value == 0x47)) value xor 1 else value

                    invertPrgA14 = newValue.bit6
                    invertChrA12 = newValue.bit7
                    currentRegister = newValue and 0x0F

                    updateState()
                }
                0x8001 -> {
                    val reg = currentRegister and if (extendedMmc3Mode) 0x0F else 0x07

                    if (reg < 12) {
                        mmc3Registers[reg] = value
                        updateState()
                    }
                }
                0xA000 -> {
                    mirroringReg = value and 0x03
                    updateState()
                }
                0xA001 -> {
                    // Ignore extra bits if bit 5 is not set.
                    val newValue = if (!value.bit5) value and 0xC0 else value

                    wramBankSelect = newValue and 0x03
                    ramInFirstChrBank = newValue.bit2
                    allowSingleScreenMirroring = newValue.bit3
                    wramConfigEnabled = newValue.bit5
                    fk23RegistersEnabled = newValue.bit6
                    wramWriteProtected = newValue.bit6
                    wramEnabled = newValue.bit7

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
    }

    override fun clock() {
        if (irqDelay > 0) {
            irqDelay--

            if (irqDelay <= 0) {
                console.cpu.setIRQSource(EXTERNAL)
            }
        }
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (a12Watcher.updateVRAMAddress(addr, console.ppu.frameCycle) == RISE) {
            if (irqCounter <= 0 || irqReload) {
                irqCounter = irqReloadValue
            } else {
                irqCounter--
            }

            if (irqCounter <= 0 && irqEnabled) {
                irqDelay = 2
            }

            irqReload = false
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("a12Watcher", a12Watcher)
        s.write("mmc3Registers", mmc3Registers)

        s.write("prgBankingMode", prgBankingMode)
        s.write("outerChrBankSize", outerChrBankSize)
        s.write("selectChrRam", selectChrRam)
        s.write("mmc3ChrMode", mmc3ChrMode)
        s.write("cnromChrMode", cnromChrMode)
        s.write("prgBaseBits", prgBaseBits)
        s.write("chrBaseBits", chrBaseBits)
        s.write("extendedMmc3Mode", extendedMmc3Mode)
        s.write("wramBankSelect", wramBankSelect)
        s.write("ramInFirstChrBank", ramInFirstChrBank)
        s.write("allowSingleScreenMirroring", allowSingleScreenMirroring)
        s.write("fk23RegistersEnabled", fk23RegistersEnabled)
        s.write("wramConfigEnabled", wramConfigEnabled)
        s.write("wramEnabled", wramEnabled)
        s.write("wramWriteProtected", wramWriteProtected)
        s.write("invertPrgA14", invertPrgA14)
        s.write("invertChrA12", invertChrA12)
        s.write("currentRegister", currentRegister)
        s.write("irqReloadValue", irqReloadValue)
        s.write("irqCounter", irqCounter)
        s.write("irqReload", irqReload)
        s.write("irqEnabled", irqEnabled)
        s.write("mirroringReg", mirroringReg)
        s.write("cnromChrReg", cnromChrReg)
        s.write("irqDelay", irqDelay)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("a12Watcher", a12Watcher)
        s.readIntArray("mmc3Registers", mmc3Registers)

        prgBankingMode = s.readInt("prgBankingMode")
        outerChrBankSize = s.readInt("outerChrBankSize")
        selectChrRam = s.readBoolean("selectChrRam")
        mmc3ChrMode = s.readBoolean("mmc3ChrMode")
        cnromChrMode = s.readBoolean("cnromChrMode")
        prgBaseBits = s.readInt("prgBaseBits")
        chrBaseBits = s.readInt("chrBaseBits")
        extendedMmc3Mode = s.readBoolean("extendedMmc3Mode")
        wramBankSelect = s.readInt("wramBankSelect")
        ramInFirstChrBank = s.readBoolean("ramInFirstChrBank")
        allowSingleScreenMirroring = s.readBoolean("allowSingleScreenMirroring")
        fk23RegistersEnabled = s.readBoolean("fk23RegistersEnabled")
        wramConfigEnabled = s.readBoolean("wramConfigEnabled")
        wramEnabled = s.readBoolean("wramEnabled")
        wramWriteProtected = s.readBoolean("wramWriteProtected")
        invertPrgA14 = s.readBoolean("invertPrgA14")
        invertChrA12 = s.readBoolean("invertChrA12")
        currentRegister = s.readInt("currentRegister")
        irqReloadValue = s.readInt("irqReloadValue")
        irqCounter = s.readInt("irqCounter")
        irqReload = s.readBoolean("irqReload")
        irqEnabled = s.readBoolean("irqEnabled")
        mirroringReg = s.readInt("mirroringReg")
        cnromChrReg = s.readInt("cnromChrReg")
        irqDelay = s.readInt("irqDelay")

        updateState()
    }
}
