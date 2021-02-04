package br.tiagohm.nestalgia.core


@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class MMC5 : Mapper() {

    private lateinit var audio: MMC5Audio
    private lateinit var mmc5MemoryHandler: MMC5MemoryHandler

    private var prgRamProtect1: UByte = 0U
    private var prgRamProtect2: UByte = 0U

    private var fillModeTile: UByte = 0U
    private var fillModeColor: UByte = 0U

    private var verticalSplitEnabled = false
    private var verticalSplitRightSide = false
    private var verticalSplitDelimiterTile: UByte = 0U
    private var verticalSplitScroll: UByte = 0U
    private var verticalSplitBank: UByte = 0U

    private var splitInSplitRegion = false
    private var splitVerticalScroll = 0U
    private var splitTile = 0U
    private var splitTileNumber = 0

    private var multiplierValue1: UByte = 0U
    private var multiplierValue2: UByte = 0U

    private var nametableMapping: UByte = 0U
    private var extendedRamMode: UByte = 0U

    // Extended attribute mode fields (used when _extendedRamMode == 1)
    private var exAttributeLastNametableFetch: UShort = 0U
    private var exAttrLastFetchCounter: Byte = 0
    private var exAttrSelectedChrBank: UByte = 0U

    private var prgMode: UByte = 0U
    private val prgBank = UByteArray(5)

    private var chrMode: UByte = 0U
    private var chrUpperBits: UByte = 0U
    private val chrBank = UShortArray(12)
    private var lastChrReg: UShort = 0U
    private var prevChrA = false

    private var irqCounterTarget: UByte = 0U
    private var irqEnabled = false
    private var scanlineCounter: UByte = 0U
    private var irqPending = false

    private var needInFrame = false
    private var ppuInFrame = false
    private var ppuIdleCounter: UByte = 0U
    private var lastPpuReadAddr: UShort = 0U
    private var ntReadCounter: UByte = 0U

    override val prgPageSize = 0x2000U

    override val chrPageSize = 0x400U

    override val registerStartAddress: UShort = 0x5000U

    override val registerEndAddress: UShort = 0x5206U

    override val saveRamPageSize = 0x2000U

    override val workRamPageSize = 0x2000U

    override val isForceSaveRamSize = true

    override val isForceWorkRamSize = true

    override val saveRamSize: UInt
        get() {
            return when {
                isNes20 -> info.header.saveRamSize.toUInt()
                info.isInDatabase -> info.gameInfo!!.saveRamSize.toUInt()
                // Emulate as if a single 64k block of work/save ram existed
                info.hasBattery -> 0x10000U
                else -> 0U
                // If there's a battery on the board, exram gets saved, too.
            } + if (hasBattery) EXRAM_SIZE.toUInt() else 0U
        }

    override val workRamSize: UInt
        get() {
            return when {
                isNes20 -> info.header.workRamSize.toUInt()
                info.isInDatabase -> info.gameInfo!!.workRamSize.toUInt()
                // Emulate as if a single 64k block of work/save ram existed (+ 1kb of ExRAM)
                info.hasBattery -> 0U
                else -> 0x10000U
                // If there's a battery on the board, exram gets saved, too.
            } + if (hasBattery) 0U else EXRAM_SIZE.toUInt()
        }

    override val allowRegisterRead = true

    override fun init() {
        addRegisterRange(0xFFFAU, 0xFFFBU, MemoryOperation.READ)

        audio = MMC5Audio(console)

        // Override the 2000-2007 registers to catch all writes to the PPU registers (but not their mirrors)
        mmc5MemoryHandler = MMC5MemoryHandler(console)

        ppuIdleCounter = 0U
        lastPpuReadAddr = 0U
        ntReadCounter = 0U
        prevChrA = false

        chrMode = 0U
        prgRamProtect1 = 0U
        prgRamProtect2 = 0U
        extendedRamMode = 0U
        nametableMapping = 0U
        fillModeColor = 0U
        fillModeTile = 0U
        verticalSplitScroll = 0U
        verticalSplitBank = 0U
        verticalSplitEnabled = false
        verticalSplitDelimiterTile = 0U
        verticalSplitRightSide = false
        multiplierValue1 = 0U
        multiplierValue2 = 0U
        chrUpperBits = 0U
        chrBank.fill(0U)
        lastChrReg = 0U

        exAttrLastFetchCounter = 0
        exAttributeLastNametableFetch = 0U
        exAttrSelectedChrBank = 0U

        irqPending = false
        irqCounterTarget = 0U
        scanlineCounter = 0U
        irqEnabled = false
        ppuInFrame = false
        needInFrame = false

        splitInSplitRegion = false
        splitVerticalScroll = 0U
        splitTile = 0U
        splitTileNumber = -1

        getNametable(NT_EMPTY_INDEX).fill(0U, NAMETABLE_SIZE)

        setExtendedRamMode(0)

        // Additionally, Romance of the 3 Kingdoms 2 seems to expect it to be in 8k PRG mode ($5100 = $03)
        writeRegister(0x5100U, 0x03U)

        // Games seem to expect $5117 to be $FF on powerup (last PRG page swapped in)
        writeRegister(0x5117U, 0xFFU)

        updateChrBanks(true)
    }

    override fun reset(softReset: Boolean) {
        console.memoryManager.registerWriteHandler(mmc5MemoryHandler, 0x2000, 0x2007)
    }

    private inline fun switchPrgBank(addr: UShort, value: UByte) {
        prgBank[addr.toInt() - 0x5113] = value
        updatePrgBanks()
    }

    private inline fun switchChrBank(addr: UShort, value: UByte) {
        val newValue = makeUShort(value, chrUpperBits)
        val reg = addr.toInt() - 0x5120

        if (newValue != chrBank[reg] || lastChrReg != addr) {
            chrBank[reg] = newValue
            lastChrReg = addr
            updateChrBanks(true)
        }
    }

    override fun processCpuClock() {
        audio.clock()

        if (ppuIdleCounter > 0U) {
            ppuIdleCounter--

            if (ppuIdleCounter.isZero) {
                // The "in-frame" flag is cleared when the PPU is no longer rendering. This is detected when
                // 3 CPU cycles pass without a PPU read having occurred (PPU /RD has not been low during the last 3 M2 rises)
                ppuInFrame = false
                updateChrBanks(true)
            }
        }
    }

    private inline fun setFillModeTile(tile: UByte) {
        fillModeTile = tile
        getNametable(NT_FILL_MODE_INDEX).fill(tile, 32 * 30) // 32 tiles per row, 30 rows
    }

    private inline fun setFillModeColor(color: UByte) {
        fillModeColor = color
        val c = color.toUInt()
        val attributeByte = (c or (c shl 2) or (c shl 4) or (c shl 6)).toUByte()
        getNametable(NT_FILL_MODE_INDEX).fill(attributeByte, 64, 32 * 30) // Attribute table is 64 bytes
    }

    private fun setNametableMapping(value: UByte) {
        nametableMapping = value

        val nametables = intArrayOf(
            0,  // 0 - On-board VRAM page 0
            1,  // 1 - On-board VRAM page 1
            // 2 - Internal Expansion RAM, only if the Extended RAM mode allows it ($5104 is 00/01);
            // otherwise, the nametable will read as all zeros
            if (extendedRamMode <= 1U) NT_WRAM_INDEX else NT_EMPTY_INDEX,
            NT_FILL_MODE_INDEX // 3 - Fill-mode data
        )

        for (i in 0..3) {
            val nametableId = nametables[((value shr (i * 2)) and 0x03U).toInt()]

            if (nametableId == NT_WRAM_INDEX) {
                val source = if (hasBattery) Pointer(saveRam, privateSaveRamSize.toInt() - EXRAM_SIZE)
                else Pointer(workRam, privateWorkRamSize.toInt() - EXRAM_SIZE)

                setPpuMemoryMapping(
                    (0x2000 + i * 0x400).toUShort(),
                    (0x2000 + i * 0x400 + 0x3FF).toUShort(),
                    source,
                    MemoryAccessType.READ_WRITE
                )
            } else {
                setNametable(i, nametableId)
            }
        }
    }

    private fun setExtendedRamMode(mode: UByte) {
        extendedRamMode = mode

        val accessType = when (mode.toInt()) {
            // Mode 0/1 - Not readable (returns open bus), can only be written while the PPU is rendering (otherwise, 0 is written)
            // See overridden WriteRam function for implementation
            0, 1 -> MemoryAccessType.WRITE
            // Mode 2 - Readable and writable
            2 -> MemoryAccessType.READ_WRITE
            // Mode 3 - Read-only
            else -> MemoryAccessType.READ
        }

        if (hasBattery) {
            setCpuMemoryMapping(
                0x5C00U,
                0x5FFFU,
                PrgMemoryType.SRAM,
                privateSaveRamSize.toInt() - EXRAM_SIZE,
                accessType
            )
        } else {
            setCpuMemoryMapping(
                0x5C00U,
                0x5FFFU,
                PrgMemoryType.WRAM,
                privateWorkRamSize.toInt() - EXRAM_SIZE,
                accessType
            )
        }

        setNametableMapping(nametableMapping)
    }

    private fun updateChrBanks(force: Boolean) {
        val largeSprites = mmc5MemoryHandler.readRegister(0x2000U).bit5

        if (!largeSprites) {
            // Using 8x8 sprites resets the last written to bank logic
            lastChrReg = 0U
        }

        val chrA = !largeSprites || splitTileNumber in 32..39 || !ppuInFrame && lastChrReg <= 0x5127U

        if (!force && chrA == prevChrA) {
            return
        }

        prevChrA = chrA

        when (chrMode.toInt()) {
            0 -> selectChrPage8x(0U, (chrBank[if (chrA) 0x07 else 0x0B].toUInt() shl 3).toUShort())
            1 -> {
                selectChrPage4x(0U, (chrBank[if (chrA) 0x03 else 0x0B].toUInt() shl 2).toUShort())
                selectChrPage4x(1U, (chrBank[if (chrA) 0x07 else 0x0B].toUInt() shl 2).toUShort())
            }
            2 -> {
                selectChrPage2x(0U, (chrBank[if (chrA) 0x01 else 0x09].toUInt() shl 1).toUShort())
                selectChrPage2x(1U, (chrBank[if (chrA) 0x03 else 0x0B].toUInt() shl 1).toUShort())
                selectChrPage2x(2U, (chrBank[if (chrA) 0x05 else 0x09].toUInt() shl 1).toUShort())
                selectChrPage2x(3U, (chrBank[if (chrA) 0x07 else 0x0B].toUInt() shl 1).toUShort())
            }
            else -> {
                selectChrPage(0U, chrBank[if (chrA) 0x00 else 0x08])
                selectChrPage(1U, chrBank[if (chrA) 0x01 else 0x09])
                selectChrPage(2U, chrBank[if (chrA) 0x02 else 0x0A])
                selectChrPage(3U, chrBank[if (chrA) 0x03 else 0x0B])
                selectChrPage(4U, chrBank[if (chrA) 0x04 else 0x08])
                selectChrPage(5U, chrBank[if (chrA) 0x05 else 0x09])
                selectChrPage(6U, chrBank[if (chrA) 0x06 else 0x0A])
                selectChrPage(7U, chrBank[if (chrA) 0x07 else 0x0B])
            }
        }
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        if (addr >= 0x5C00U && addr <= 0x5FFFU && extendedRamMode <= 1U && !ppuInFrame) {
            // Expansion RAM ($5C00-$5FFF, read/write)
            // Mode 0/1 - Not readable (returns open bus), can only be written while the PPU is rendering (otherwise, 0 is written)
            super.write(addr, 0U, type)
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