package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_001

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
open class MMC1 : Mapper() {

    private var writeBuffer: UByte = 0U
    private var shiftCount = 0
    private var lastWriteCycle = 0L
    private var forceWramOn = false
    private var lastChrReg = 1

    protected val state = UByteArray(4)

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

    protected inline var stateC000: UByte
        get() = state[2]
        set(value) {
            state[2] = value
        }

    protected inline var stateE000: UByte
        get() = state[3]
        set(value) {
            state[3] = value
        }

    override val prgPageSize = 0x4000U

    override val chrPageSize = 0x1000U

    override fun init() {
        super.init()

        // On powerup: bits 2,3 of $8000 are set (this ensures the $8000 is bank 0,
        // and $C000 is the last bank - needed for SEROM/SHROM/SH1ROM which do no support banking)
        state8000 = getPowerOnByte() or 0x0CU
        stateA000 = getPowerOnByte()
        stateC000 = getPowerOnByte()
        // WRAM Disable: enabled by default for MMC1B
        stateE000 = if (info.gameInfo?.board?.contains("MMC1B") == true) 0x10U else 0x00U
        // MMC1A: PRG RAM is always enabled - Normally these roms should be classified as mapper 155
        forceWramOn = info.gameInfo?.board == "MMC1A"
        lastChrReg = 1 // 0xA000

        updateState()
    }

    private inline val UByte.hasResetFlag: Boolean
        get() = this.bit7

    private inline fun resetBuffer() {
        shiftCount = 0
        writeBuffer = 0U
    }

    private inline fun isBufferFull(value: UByte): Boolean {
        if (value.hasResetFlag) {
            // When 'r' is set:
            //	- 'd' is ignored
            //	- hidden temporary reg is reset (so that the next write is the "first" write)
            //	- bits 2,3 of reg $8000 are set (16k PRG mode, $8000 swappable)
            //	- other bits of $8000 (and other regs) are unchanged
            resetBuffer()
            state8000 = state8000 or 0x0CU
            updateState()
            return false
        } else {
            writeBuffer = writeBuffer shr 1
            writeBuffer = writeBuffer or ((value.toInt() shl 4) and 0x10).toUByte()

            shiftCount++

            return shiftCount == 5
        }
    }

    protected open fun updateState() {
        mirroringType = when (state8000.toInt() and 0x03) {
            0 -> MirroringType.SCREEN_A_ONLY
            1 -> MirroringType.SCREEN_B_ONLY
            2 -> MirroringType.VERTICAL
            else -> MirroringType.HORIZONTAL
        }

        val wramDisable = stateE000.bit4

        val is8000 = state8000.bit2 // Slot address: 0x8000 or 0xC000
        val isPrg16k = state8000.bit3
        val isChr4k = state8000.bit4

        val chrReg0 = stateA000 and 0x1FU
        val chrReg1 = stateC000 and 0x1FU
        val prgReg = stateE000 and 0x0FU

        val extraReg = if (lastChrReg == 2 && isChr4k) chrReg1 else chrReg0
        // 512kb carts use bit 7 of $A000/$C000 to select page
        // This is used for SUROM (Dragon Warrior 3/4, Dragon Quest 4)
        val prgBankSelect = if (privatePrgSize == 0x80000U) extraReg and 0x10U else 0U

        val accessType = if (wramDisable && !forceWramOn) MemoryAccessType.NO_ACCESS else MemoryAccessType.READ_WRITE
        val memoryType = if (hasBattery) PrgMemoryType.SRAM else PrgMemoryType.WRAM

        val size = privateSaveRamSize + privateWorkRamSize

        if (size > 0x4000U) {
            // SXROM, 32kb of save ram
            setCpuMemoryMapping(0x6000U, 0x7FFFU, ((extraReg shr 2) and 0x03U).toShort(), memoryType, accessType)
        } else if (size > 0x2000U) {
            if (privateSaveRamSize == 0x2000U && privateWorkRamSize == 0x2000U) {
                // SOROM, half of the 16kb ram is battery backed
                setCpuMemoryMapping(
                    0x6000U,
                    0x7FFFU,
                    0,
                    if (extraReg.bit3) PrgMemoryType.WRAM else PrgMemoryType.SRAM,
                    accessType
                )
            } else {
                // Unknown, shouldn't happen
                setCpuMemoryMapping(0x6000U, 0x7FFFU, ((extraReg shr 2) and 0x01U).toShort(), memoryType, accessType)
            }
        } else {
            // Everything else - 8kb of work or save ram
            setCpuMemoryMapping(0x6000U, 0x7FFFU, 0, memoryType, accessType)
        }

        if (info.subMapperId == 5) {
            // 001: 5 Fixed PRG    SEROM, SHROM, SH1ROM use a fixed 32k PRG ROM with no banking support.
            selectPrgPage2x(0U, 0U)
        }
        // 16K
        else if (isPrg16k) {
            if (is8000) {
                selectPrgPage(0U, (prgReg or prgBankSelect).toUShort())
                selectPrgPage(1U, (0x0FU.toUByte() or prgBankSelect).toUShort())
            } else {
                selectPrgPage(0U, prgBankSelect.toUShort())
                selectPrgPage(1U, (prgReg or prgBankSelect).toUShort())
            }
        }
        // 32K
        else {
            selectPrgPage2x(0U, ((prgReg and 0xFEU) or prgBankSelect).toUShort())
        }

        if (isChr4k) {
            selectChrPage(0U, chrReg0.toUShort())
            selectChrPage(1U, chrReg1.toUShort())
        } else {
            val page = chrReg0 and 0x1EU
            selectChrPage(0U, page.toUShort())
            selectChrPage(1U, (page + 1U).toUShort())
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        val cycle = console.cpu.cycleCount

        // Ignore write if within 2 cycles of another write (i.e the real write after a dummy write)
        if (cycle - lastWriteCycle >= 2) {
            if (isBufferFull(value)) {
                val a = ((addr and 0x6000U) shr 13).toInt()

                state[a] = writeBuffer

                if (a == 1 || a == 2) {
                    lastChrReg = a
                }

                updateState()
                // Reset buffer after writing 5 bits
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

        s.readUByteArray("state")?.copyInto(state)
        writeBuffer = s.readUByte("writeBuffer") ?: 0U
        shiftCount = s.readInt("shiftCount") ?: 0
        lastWriteCycle = s.readLong("lastWriteCycle") ?: 0L
        lastChrReg = s.readInt("lastChrReg") ?: 1

        updateState()
    }
}