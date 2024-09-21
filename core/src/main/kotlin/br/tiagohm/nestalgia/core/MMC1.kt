package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_001

open class MMC1(console: Console) : Mapper(console) {

    @Volatile @JvmField protected var writeBuffer = 0
    @Volatile @JvmField protected var shiftCount = 0

    @Volatile @JvmField protected var wramDisable = false
    @Volatile @JvmField protected var chrMode = false
    @Volatile @JvmField protected var prgMode = false
    @Volatile @JvmField protected var slotSelect = false

    @Volatile @JvmField protected var chrReg0 = 0
    @Volatile @JvmField protected var chrReg1 = 0
    @Volatile @JvmField protected var prgReg = 0

    @Volatile @JvmField protected var lastWriteCycle = 0L

    @Volatile @JvmField protected var forceWramOn = false
    @Volatile @JvmField protected var lastChrReg = 0

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x1000

    override fun initialize() {
        // On powerup: bits 2,3 of $8000 are set (this ensures the $8000 is bank 0, and $C000 is the last bank - needed for SEROM/SHROM/SH1ROM which do no support banking)
        processRegisterWrite(0x8000, powerOnByte() or 0x0C)
        processRegisterWrite(0xA000, powerOnByte())
        processRegisterWrite(0xC000, powerOnByte())
        // WRAM Disable: enabled by default for MMC1B
        processRegisterWrite(0xE000, if (info.gameInfo?.board?.contains("MMC1B") == true) 0x10 else 0x00)

        //"MMC1A: PRG RAM is always enabled" - Normally these roms should be classified as mapper 155
        forceWramOn = info.gameInfo?.board == "MMC1A"
        lastChrReg = 0xA000

        updateState()
    }

    private fun resetBuffer() {
        shiftCount = 0
        writeBuffer = 0
    }

    protected open fun updateState() {
        val extraReg = if (lastChrReg == 0xC000 && chrMode) chrReg1 else chrReg0
        var prgBankSelect = 0

        if (mPrgSize == 0x80000) {
            // 512kb carts use bit 7 of $A000/$C000 to select page
            // This is used for SUROM (Dragon Warrior 3/4, Dragon Quest 4)
            prgBankSelect = extraReg and 0x10
        }

        val access = if (wramDisable && !forceWramOn) MemoryAccessType.NO_ACCESS else MemoryAccessType.READ_WRITE
        val memType = if (hasBattery) PrgMemoryType.SRAM else PrgMemoryType.WRAM

        if (mSaveRamSize + mWorkRamSize > 0x4000) {
            // SXROM, 32kb of save ram
            addCpuMemoryMapping(0x6000, 0x7FFF, extraReg shr 2 and 0x03, memType, access)
        } else if (mSaveRamSize + mWorkRamSize > 0x2000) {
            if (mSaveRamSize == 0x2000 && mWorkRamSize == 0x2000) {
                //SOROM, half of the 16kb ram is battery backed
                addCpuMemoryMapping(0x6000, 0x7FFF, 0, if ((extraReg shr 3).bit0) PrgMemoryType.WRAM else PrgMemoryType.SRAM, access)
            } else {
                //Unknown, shouldn't happen
                addCpuMemoryMapping(0x6000, 0x7FFF, (extraReg shr 2) and 0x01, memType, access)
            }
        } else {
            if (mSaveRamSize + mWorkRamSize == 0) {
                removeCpuMemoryMapping(0x6000, 0x7FFF)
            } else {
                // Everything else - 8kb of work or save ram
                addCpuMemoryMapping(0x6000, 0x7FFF, 0, memType, access)
            }
        }

        if (data.info.subMapperId == 5) {
            // SubMapper 5
            // "001: 5 Fixed PRG    SEROM, SHROM, SH1ROM use a fixed 32k PRG ROM with no banking support.
            selectPrgPage2x(0, 0)
        } else {
            if (prgMode) {
                if (slotSelect) {
                    selectPrgPage(0, prgReg or prgBankSelect)
                    selectPrgPage(1, 0x0F or prgBankSelect)
                } else {
                    selectPrgPage(0, 0 or prgBankSelect)
                    selectPrgPage(1, prgReg or prgBankSelect)
                }
            } else {
                selectPrgPage2x(0, (prgReg and 0xFE) or prgBankSelect)
            }
        }

        if (chrMode) {
            selectChrPage(0, chrReg0)
            selectChrPage(1, chrReg1)
        } else {
            selectChrPage(0, chrReg0 and 0x1E)
            selectChrPage(1, (chrReg0 and 0x1E) + 1)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        val currentCycle = console.masterClock

        // Ignore write if within 2 cycles of another write (i.e the real write after a dummy write)
        // If the reset bit is set, process the write even if another write just occurred (fixes bug in Shinsenden)
        if (value.bit7 || currentCycle - lastWriteCycle >= 2) {
            processBitWrite(addr, value)
        }

        lastWriteCycle = currentCycle
    }

    private fun processBitWrite(addr: Int, value: Int) {
        if (value.bit7) {
            // When 'r' is set:
            //	- 'd' is ignored
            //	- hidden temporary reg is reset (so that the next write is the "first" write)
            //	- bits 2,3 of reg $8000 are set (16k PRG mode, $8000 swappable)
            //	- other bits of $8000 (and other regs) are unchanged
            resetBuffer()
            prgMode = true
            slotSelect = true
            updateState()
        } else {
            writeBuffer = writeBuffer shr 1
            writeBuffer = writeBuffer or ((value shl 4) and 0x10)

            shiftCount++

            if (shiftCount == 5) {
                processRegisterWrite(addr, writeBuffer)
                updateState()

                // Reset buffer after writing 5 bits
                resetBuffer()
            }
        }
    }

    private fun processRegisterWrite(addr: Int, value: Int) {
        when (addr and 0xE000) {
            0x8000 -> {
                mirroringType = when (value and 0x03) {
                    0 -> SCREEN_A_ONLY
                    1 -> SCREEN_B_ONLY
                    2 -> VERTICAL
                    else -> HORIZONTAL
                }

                slotSelect = value.bit2
                prgMode = value.bit3
                chrMode = value.bit4
            }
            0xA000 -> {
                lastChrReg = addr
                chrReg0 = value and 0x1F
            }
            0xC000 -> {
                lastChrReg = addr
                chrReg1 = value and 0x1F
            }
            0xE000 -> {
                prgReg = value and 0x0F
                wramDisable = value.bit4
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("writeBuffer", writeBuffer)
        s.write("shiftCount", shiftCount)
        s.write("wramDisable", wramDisable)
        s.write("chrMode", chrMode)
        s.write("prgMode", prgMode)
        s.write("slotSelect", slotSelect)
        s.write("chrReg0", chrReg0)
        s.write("chrReg1", chrReg1)
        s.write("prgReg", prgReg)
        s.write("lastWriteCycle", lastWriteCycle)
        s.write("lastChrReg", lastChrReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        writeBuffer = s.readInt("writeBuffer")
        shiftCount = s.readInt("shiftCount")
        wramDisable = s.readBoolean("wramDisable")
        chrMode = s.readBoolean("chrMode")
        prgMode = s.readBoolean("prgMode")
        slotSelect = s.readBoolean("slotSelect")
        chrReg0 = s.readInt("chrReg0")
        chrReg1 = s.readInt("chrReg1")
        prgReg = s.readInt("prgReg")
        lastWriteCycle = s.readLong("lastWriteCycle")
        lastChrReg = s.readInt("lastChrReg", 0xA000)
    }
}
