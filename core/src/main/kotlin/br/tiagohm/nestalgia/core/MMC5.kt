package br.tiagohm.nestalgia.core

class MMC5 : Mapper() {

    private lateinit var audio: MMC5Audio
    private lateinit var mmc5MemoryHandler: MMC5MemoryHandler

    private var prgRamProtect1 = 0
    private var prgRamProtect2 = 0

    private var fillModeTile = 0
    private var fillModeColor = 0

    private var verticalSplitEnabled = false
    private var verticalSplitRightSide = false
    private var verticalSplitDelimiterTile = 0
    private var verticalSplitScroll = 0
    private var verticalSplitBank = 0

    private var splitInSplitRegion = false
    private var splitVerticalScroll = 0
    private var splitTile = 0
    private var splitTileNumber = 0

    private var multiplierValue1 = 0
    private var multiplierValue2 = 0

    private var nametableMapping = 0
    private var extendedRamMode = 0

    // Extended attribute mode fields (used when _extendedRamMode == 1)
    private var exAttributeLastNametableFetch = 0
    private var exAttrLastFetchCounter = 0
    private var exAttrSelectedChrBank = 0

    private var prgMode = 0
    private val prgBank = IntArray(5)

    private var chrMode = 0
    private var chrUpperBits = 0
    private val chrBank = IntArray(12)
    private var lastChrReg = 0
    private var prevChrA = false

    private var irqCounterTarget = 0
    private var irqEnabled = false
    private var scanlineCounter = 0
    private var irqPending = false

    private var needInFrame = false
    private var ppuInFrame = false
    private var ppuIdleCounter = 0
    private var lastPpuReadAddr = 0
    private var ntReadCounter = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val registerStartAddress = 0x5000

    override val registerEndAddress = 0x5206

    override val saveRamPageSize = 0x2000

    override val workRamPageSize = 0x2000

    override val isForceSaveRamSize = true

    override val isForceWorkRamSize = true

    override val saveRamSize: Int
        get() {
            return when {
                isNes20 -> info.header.saveRamSize
                info.isInDatabase -> info.gameInfo!!.saveRamSize
                // Emulate as if a single 64k block of work/save ram existed
                info.hasBattery -> 0x10000
                else -> 0
                // If there's a battery on the board, exram gets saved, too.
            } + if (hasBattery) EXRAM_SIZE else 0
        }

    override val workRamSize: Int
        get() {
            return when {
                isNes20 -> info.header.workRamSize
                info.isInDatabase -> info.gameInfo!!.workRamSize
                // Emulate as if a single 64k block of work/save ram existed (+ 1kb of ExRAM)
                info.hasBattery -> 0
                else -> 0x10000
                // If there's a battery on the board, exram gets saved, too.
            } + if (hasBattery) 0 else EXRAM_SIZE
        }

    override val allowRegisterRead = true

    override fun initialize() {
        addRegisterRange(0xFFFA, 0xFFFB, MemoryOperation.READ)

        audio = MMC5Audio(console)

        // Override the 2000-2007 registers to catch all writes to the PPU registers (but not their mirrors)
        mmc5MemoryHandler = MMC5MemoryHandler(console)

        ppuIdleCounter = 0
        lastPpuReadAddr = 0
        ntReadCounter = 0
        prevChrA = false

        chrMode = 0
        prgRamProtect1 = 0
        prgRamProtect2 = 0
        extendedRamMode = 0
        nametableMapping = 0
        fillModeColor = 0
        fillModeTile = 0
        verticalSplitScroll = 0
        verticalSplitBank = 0
        verticalSplitEnabled = false
        verticalSplitDelimiterTile = 0
        verticalSplitRightSide = false
        multiplierValue1 = 0
        multiplierValue2 = 0
        chrUpperBits = 0
        chrBank.fill(0)
        lastChrReg = 0

        exAttrLastFetchCounter = 0
        exAttributeLastNametableFetch = 0
        exAttrSelectedChrBank = 0

        irqPending = false
        irqCounterTarget = 0
        scanlineCounter = 0
        irqEnabled = false
        ppuInFrame = false
        needInFrame = false

        splitInSplitRegion = false
        splitVerticalScroll = 0
        splitTile = 0
        splitTileNumber = -1

        nametableAt(NT_EMPTY_INDEX).fill(0, NAMETABLE_SIZE)

        extendedRamMode(0)

        // Additionally, Romance of the 3 Kingdoms 2 seems to expect it to be in 8k PRG mode ($5100 = $03)
        writeRegister(0x5100, 0x03)

        // Games seem to expect $5117 to be $FF on powerup (last PRG page swapped in)
        writeRegister(0x5117, 0xFF)

        updateChrBanks(true)
    }

    override fun reset(softReset: Boolean) {
        console.memoryManager.registerWriteHandler(mmc5MemoryHandler, 0x2000, 0x2007)
    }

    private fun switchPrgBank(addr: Int, value: Int) {
        prgBank[addr - 0x5113] = value
        // TODO: updatePrgBanks()
    }

    private fun switchChrBank(addr: Int, value: Int) {
        val newValue = value or (chrUpperBits shl 8)
        val reg = addr - 0x5120

        if (newValue != chrBank[reg] || lastChrReg != addr) {
            chrBank[reg] = newValue
            lastChrReg = addr
            updateChrBanks(true)
        }
    }

    override fun processCpuClock() {
        audio.clock()

        if (ppuIdleCounter > 0) {
            ppuIdleCounter--

            if (ppuIdleCounter == 0) {
                // The "in-frame" flag is cleared when the PPU is no longer rendering. This is detected when
                // 3 CPU cycles pass without a PPU read having occurred (PPU /RD has not been low during the last 3 M2 rises)
                ppuInFrame = false
                updateChrBanks(true)
            }
        }
    }

    private fun fillModeTile(tile: Int) {
        fillModeTile = tile
        nametableAt(NT_FILL_MODE_INDEX).fill(tile, 32 * 30) // 32 tiles per row, 30 rows
    }

    private fun fillModeColor(color: Int) {
        fillModeColor = color
        val attributeByte = color or (color shl 2) or (color shl 4) or (color shl 6)
        nametableAt(NT_FILL_MODE_INDEX).fill(attributeByte, 64, 32 * 30) // Attribute table is 64 bytes
    }

    private fun nametableMapping(value: Int) {
        nametableMapping = value

        val nametables = intArrayOf(
            0,  // 0 - On-board VRAM page 0
            1,  // 1 - On-board VRAM page 1
            // 2 - Internal Expansion RAM, only if the Extended RAM mode allows it ($5104 is 00/01);
            // otherwise, the nametable will read as all zeros
            if (extendedRamMode <= 1) NT_WRAM_INDEX else NT_EMPTY_INDEX,
            NT_FILL_MODE_INDEX // 3 - Fill-mode data
        )

        for (i in 0..3) {
            val nametableId = nametables[value shr (i * 2) and 0x03]

            if (nametableId == NT_WRAM_INDEX) {
                val source = if (hasBattery) Pointer(saveRam, mSaveRamSize - EXRAM_SIZE)
                else Pointer(workRam, mWorkRamSize - EXRAM_SIZE)

                addPpuMemoryMapping(
                    0x2000 + i * 0x400,
                    0x2000 + i * 0x400 + 0x3FF,
                    source,
                    MemoryAccessType.READ_WRITE
                )
            } else {
                nametable(i, nametableId)
            }
        }
    }

    private fun extendedRamMode(mode: Int) {
        extendedRamMode = mode

        val accessType = when (mode) {
            // Mode 0/1 - Not readable (returns open bus), can only be written while the PPU is rendering (otherwise, 0 is written)
            // See overridden WriteRam function for implementation
            0, 1 -> MemoryAccessType.WRITE
            // Mode 2 - Readable and writable
            2 -> MemoryAccessType.READ_WRITE
            // Mode 3 - Read-only
            else -> MemoryAccessType.READ
        }

        if (hasBattery) {
            addCpuMemoryMapping(
                0x5C00,
                0x5FFF,
                PrgMemoryType.SRAM,
                mSaveRamSize - EXRAM_SIZE,
                accessType
            )
        } else {
            addCpuMemoryMapping(
                0x5C00,
                0x5FFF,
                PrgMemoryType.WRAM,
                mWorkRamSize - EXRAM_SIZE,
                accessType
            )
        }

        nametableMapping(nametableMapping)
    }

    private fun updateChrBanks(force: Boolean) {
        val largeSprites = mmc5MemoryHandler.readRegister(0x2000).bit5

        if (!largeSprites) {
            // Using 8x8 sprites resets the last written to bank logic
            lastChrReg = 0
        }

        val chrA = !largeSprites || splitTileNumber in 32..39 || !ppuInFrame && lastChrReg <= 0x5127

        if (!force && chrA == prevChrA) {
            return
        }

        prevChrA = chrA

        when (chrMode) {
            0 -> selectChrPage8x(0, chrBank[if (chrA) 0x07 else 0x0B] shl 3)
            1 -> {
                selectChrPage4x(0, chrBank[if (chrA) 0x03 else 0x0B] shl 2)
                selectChrPage4x(1, chrBank[if (chrA) 0x07 else 0x0B] shl 2)
            }
            2 -> {
                selectChrPage2x(0, chrBank[if (chrA) 0x01 else 0x09] shl 1)
                selectChrPage2x(1, chrBank[if (chrA) 0x03 else 0x0B] shl 1)
                selectChrPage2x(2, chrBank[if (chrA) 0x05 else 0x09] shl 1)
                selectChrPage2x(3, chrBank[if (chrA) 0x07 else 0x0B] shl 1)
            }
            else -> {
                selectChrPage(0, chrBank[if (chrA) 0x00 else 0x08])
                selectChrPage(1, chrBank[if (chrA) 0x01 else 0x09])
                selectChrPage(2, chrBank[if (chrA) 0x02 else 0x0A])
                selectChrPage(3, chrBank[if (chrA) 0x03 else 0x0B])
                selectChrPage(4, chrBank[if (chrA) 0x04 else 0x08])
                selectChrPage(5, chrBank[if (chrA) 0x05 else 0x09])
                selectChrPage(6, chrBank[if (chrA) 0x06 else 0x0A])
                selectChrPage(7, chrBank[if (chrA) 0x07 else 0x0B])
            }
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        if (addr in 0x5C00..0x5FFF && extendedRamMode <= 1 && !ppuInFrame) {
            // Expansion RAM ($5C00-$5FFF, read/write)
            // Mode 0/1 - Not readable (returns open bus), can only be written while the PPU is rendering (otherwise, 0 is written)
            super.write(addr, 0, type)
        } else {
            super.write(addr, value, type)
        }
    }

    companion object {

        private const val EXRAM_SIZE = 0x400
        private const val NT_WRAM_INDEX = 4
        private const val NT_EMPTY_INDEX = 2
        private const val NT_FILL_MODE_INDEX = 3
    }
}
