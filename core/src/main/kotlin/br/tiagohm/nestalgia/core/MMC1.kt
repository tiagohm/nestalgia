package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_001

open class MMC1(console: Console) : Mapper(console) {

    private var writeBuffer = 0
    private var shiftCount = 0
    private var lastWriteCycle = 0L
    private var forceWramOn = false
    private var lastChrReg = 1

    protected val state = IntArray(4)

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

    protected inline var stateC000
        get() = state[2]
        set(value) {
            state[2] = value
        }

    protected inline var stateE000
        get() = state[3]
        set(value) {
            state[3] = value
        }

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x1000

    override fun initialize() {
        // On powerup: bits 2,3 of $8000 are set (this ensures the $8000 is bank 0,
        // and $C000 is the last bank - needed for SEROM/SHROM/SH1ROM which do no support banking)
        state8000 = powerOnByte() or 0x0C
        stateA000 = powerOnByte()
        stateC000 = powerOnByte()
        // WRAM Disable: enabled by default for MMC1B
        stateE000 = if (info.gameInfo?.board?.contains("MMC1B") == true) 0x10 else 0x00
        // MMC1A: PRG RAM is always enabled - Normally these roms should be classified as mapper 155
        forceWramOn = info.gameInfo?.board == "MMC1A"
        lastChrReg = 1 // 0xA000

        updateState()
    }

    private fun resetBuffer() {
        shiftCount = 0
        writeBuffer = 0
    }

    private fun isBufferFull(value: Int): Boolean {
        if (value.hasResetFlag) {
            // When 'r' is set:
            //	- 'd' is ignored
            //	- hidden temporary reg is reset (so that the next write is the "first" write)
            //	- bits 2,3 of reg $8000 are set (16k PRG mode, $8000 swappable)
            //	- other bits of $8000 (and other regs) are unchanged
            resetBuffer()
            state8000 = state8000 or 0x0C
            updateState()
            return false
        } else {
            writeBuffer = writeBuffer shr 1
            writeBuffer = writeBuffer or (value shl 4 and 0x10)

            shiftCount++

            return shiftCount == 5
        }
    }

    protected open fun updateState() {
        mirroringType = when (state8000 and 0x03) {
            0 -> MirroringType.SCREEN_A_ONLY
            1 -> MirroringType.SCREEN_B_ONLY
            2 -> MirroringType.VERTICAL
            else -> MirroringType.HORIZONTAL
        }

        val wramDisable = stateE000.bit4

        val is8000 = state8000.bit2 // Slot address: 0x8000 or 0xC000
        val isPrg16k = state8000.bit3
        val isChr4k = state8000.bit4

        val chrReg0 = stateA000 and 0x1F
        val chrReg1 = stateC000 and 0x1F
        val prgReg = stateE000 and 0x0F

        val extraReg = if (lastChrReg == 2 && isChr4k) chrReg1 else chrReg0
        // 512kb carts use bit 7 of $A000/$C000 to select page
        // This is used for SUROM (Dragon Warrior 3/4, Dragon Quest 4)
        val prgBankSelect = if (mPrgSize == 0x80000) extraReg and 0x10 else 0

        val accessType = if (wramDisable && !forceWramOn) MemoryAccessType.NO_ACCESS else MemoryAccessType.READ_WRITE
        val memoryType = if (hasBattery) PrgMemoryType.SRAM else PrgMemoryType.WRAM

        val size = mSaveRamSize + mWorkRamSize

        if (size > 0x4000) {
            // SXROM, 32kb of save ram
            addCpuMemoryMapping(0x6000, 0x7FFF, extraReg shr 2 and 0x03, memoryType, accessType)
        } else if (size > 0x2000) {
            if (mSaveRamSize == 0x2000 && mWorkRamSize == 0x2000) {
                // SOROM, half of the 16kb ram is battery backed
                addCpuMemoryMapping(
                    0x6000,
                    0x7FFF,
                    0,
                    if (extraReg.bit3) PrgMemoryType.WRAM else PrgMemoryType.SRAM,
                    accessType,
                )
            } else {
                // Unknown, shouldn't happen
                addCpuMemoryMapping(0x6000, 0x7FFF, extraReg shr 2 and 0x01, memoryType, accessType)
            }
        } else {
            // Everything else - 8kb of work or save ram
            addCpuMemoryMapping(0x6000, 0x7FFF, 0, memoryType, accessType)
        }

        if (info.subMapperId == 5) {
            // 001: 5 Fixed PRG    SEROM, SHROM, SH1ROM use a fixed 32k PRG ROM with no banking support.
            selectPrgPage2x(0, 0)
        }
        // 16K
        else if (isPrg16k) {
            if (is8000) {
                selectPrgPage(0, prgReg or prgBankSelect)
                selectPrgPage(1, 0x0F or prgBankSelect)
            } else {
                selectPrgPage(0, prgBankSelect)
                selectPrgPage(1, prgReg or prgBankSelect)
            }
        }
        // 32K
        else {
            selectPrgPage2x(0, (prgReg and 0xFE) or prgBankSelect)
        }

        if (isChr4k) {
            selectChrPage(0, chrReg0)
            selectChrPage(1, chrReg1)
        } else {
            val page = chrReg0 and 0x1E
            selectChrPage(0, page)
            selectChrPage(1, page + 1)
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        val cycle = console.cpu.cycleCount

        // Ignore write if within 2 cycles of another write (i.e the real write after a dummy write)
        if (cycle - lastWriteCycle >= 2) {
            if (isBufferFull(value)) {
                val a = addr and 0x6000 shr 13

                state[a] = writeBuffer

                if (a == 1 || a == 2) {
                    lastChrReg = a
                }

                updateState()
                // Reset buffer after writing 5 bits.
                resetBuffer()
            }
        }

        lastWriteCycle = cycle
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("state", state)
        s.write("writeBuffer", writeBuffer)
        s.write("shiftCount", shiftCount)
        s.write("lastWriteCycle", lastWriteCycle)
        s.write("lastChrReg", lastChrReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("state", state)
        writeBuffer = s.readInt("writeBuffer")
        shiftCount = s.readInt("shiftCount")
        lastWriteCycle = s.readLong("lastWriteCycle")
        lastChrReg = s.readInt("lastChrReg", 1)

        updateState()
    }

    companion object {

        private inline val Int.hasResetFlag
            get() = this.bit7
    }
}
