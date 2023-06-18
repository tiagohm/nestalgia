package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_005

class MMC5(console: Console) : Mapper(console) {

    private data class CpuBankInfo(
        @JvmField var bankNumber: Int = 0,
        @JvmField var memoryType: PrgMemoryType = ROM,
        @JvmField var accessType: MemoryAccessType = NO_ACCESS,
    )

    private val audio = MMC5Audio(console)

    // Override the 2000-2007 registers to catch all writes to the PPU registers
    // (but not their mirrors).
    private val mmc5MemoryHandler = MMC5MemoryHandler(console)

    private val fillNametable = Pointer(IntArray(NAMETABLE_SIZE))
    private val emptyNametable = Pointer(IntArray(NAMETABLE_SIZE))

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
    private var splitTileNumber = -1

    private var multiplierValue1 = 0
    private var multiplierValue2 = 0

    private var nametableMapping = 0
    private var extendedRamMode = 0

    // Extended attribute mode fields (used when _extendedRamMode == 1)
    private var exAttributeLastNametableFetch = 0
    private var exAttrLastFetchCounter = 0
    private var exAttrSelectedChrBank = 0

    private var prgMode = 0
    private val prgBanks = IntArray(5)

    private var chrMode = 0
    private var chrUpperBits = 0
    private val chrBanks = IntArray(12)
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

    override val saveRamSize
        get() = when {
            isNes20 -> info.header.saveRamSize
            info.isInDatabase -> info.gameInfo!!.saveRamSize
            // Emulate as if a single 64k block of work/save ram existed
            info.hasBattery -> 0x10000
            else -> 0
            // If there's a battery on the board, exram gets saved, too.
        } + if (hasBattery) EXRAM_SIZE else 0

    override val workRamSize
        get() = when {
            isNes20 -> info.header.workRamSize
            info.isInDatabase -> info.gameInfo!!.workRamSize
            // Emulate as if a single 64k block of work/save ram existed (+ 1kb of ExRAM)
            info.hasBattery -> 0
            else -> 0x10000
            // If there's a battery on the board, exram gets saved, too.
        } + if (hasBattery) 0 else EXRAM_SIZE

    override val allowRegisterRead = true

    override fun initialize() {
        addRegisterRange(0xFFFA, 0xFFFB, MemoryOperation.READ)

        extendedRamMode(0)

        // Additionally, Romance of the 3 Kingdoms 2 seems to expect it
        // to be in 8k PRG mode ($5100 = $03).
        writeRegister(0x5100, 0x03)

        // Games seem to expect $5117 to be $FF on powerup (last PRG page swapped in).
        writeRegister(0x5117, 0xFF)

        updateChrBanks(true)
    }

    override fun reset(softReset: Boolean) {
        console.memoryManager.registerWriteHandler(mmc5MemoryHandler, 0x2000, 0x2007)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun switchPrgBank(addr: Int, value: Int) {
        prgBanks[addr - 0x5113] = value
        updatePrgBanks()
    }

    private fun cpuBankInfo(addr: Int, cpuBankInfo: CpuBankInfo) {
        var bankNumber = prgBanks[addr - 0x5113]
        var memoryType = ROM
        var accessType: MemoryAccessType

        if (!bankNumber.bit7 && addr != 0x5117 || addr == 0x5113) {
            bankNumber = bankNumber and 0x07
            accessType = READ

            if (prgRamProtect1 == 0x02 && prgRamProtect2 == 0x01) {
                accessType = READ_WRITE
            }

            // WRAM/SRAM mirroring logic (only supports existing/known licensed MMC5 boards)
            //            Bank number
            //            0 1 2 3 4 5 6 7
            // --------------------------
            // None     : - - - - - - - -
            // 1x 8kb   : 0 0 0 0 - - - -
            // 2x 8kb   : 0 0 0 0 1 1 1 1
            // 1x 32kb  : 0 1 2 3 - - - -
            val realWorkRamSize = mWorkRamSize - if (hasBattery) 0 else EXRAM_SIZE
            val realSaveRamSize = mSaveRamSize - if (hasBattery) EXRAM_SIZE else 0

            if (isNes20 || info.isInDatabase) {
                memoryType = WRAM

                if (hasBattery && (bankNumber <= 3 || realSaveRamSize > 0x2000)) {
                    memoryType = SRAM
                }

                if (realSaveRamSize + realWorkRamSize != 0x4000 && bankNumber >= 4) {
                    // When not 2x 8kb (=16kb), banks 4/5/6/7 select the empty socket
                    // and return open bus.
                    accessType = NO_ACCESS
                }
            } else {
                memoryType = if (hasBattery) SRAM else WRAM
            }

            if (memoryType == WRAM) {
                // Properly mirror work ram (by ignoring the extra 1kb ExRAM section).
                bankNumber = bankNumber and (realWorkRamSize / 0x2000 - 1)

                if (mWorkRamSize == EXRAM_SIZE) {
                    accessType = NO_ACCESS
                }
            } else {
                // Properly mirror work ram (by ignoring the extra 1kb ExRAM section).
                bankNumber = bankNumber and (realSaveRamSize / 0x2000 - 1)

                if (mSaveRamSize == EXRAM_SIZE) {
                    accessType = NO_ACCESS
                }
            }
        } else {
            accessType = READ
            bankNumber = bankNumber and 0x7F
        }

        cpuBankInfo.bankNumber = bankNumber
        cpuBankInfo.memoryType = memoryType
        cpuBankInfo.accessType = accessType
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun addCpuMemoryMapping(
        start: Int, end: Int,
        cpuBankInfo: CpuBankInfo,
        pageNumber: Int = cpuBankInfo.bankNumber,
        memoryType: PrgMemoryType = cpuBankInfo.memoryType,
        accessType: MemoryAccessType = cpuBankInfo.accessType,
    ) {
        addCpuMemoryMapping(start, end, pageNumber, memoryType, accessType)
    }

    private fun updatePrgBanks() {
        val cpuBankInfo = CpuBankInfo()

        cpuBankInfo(0x5113, cpuBankInfo)

        if (cpuBankInfo.accessType == NO_ACCESS) {
            removeCpuMemoryMapping(0x6000, 0x7FFF)
        } else {
            addCpuMemoryMapping(0x6000, 0x7FFF, cpuBankInfo)
        }

        // PRG Bank 0
        // Mode 0,1,2 - Ignored
        // Mode 3 - Select an 8KB PRG bank at $8000-$9FFF
        if (prgMode == 3) {
            cpuBankInfo(0x5114, cpuBankInfo)
            addCpuMemoryMapping(0x8000, 0x9FFF, cpuBankInfo)
        }

        // PRG Bank 1
        // Mode 0 - Ignored
        // Mode 1,2 - Select a 16KB PRG bank at $8000-$BFFF (ignore bottom bit)
        // Mode 3 - Select an 8KB PRG bank at $A000-$BFFF
        cpuBankInfo(0x5115, cpuBankInfo)

        if (prgMode == 1 || prgMode == 2) {
            addCpuMemoryMapping(0x8000, 0xBFFF, cpuBankInfo, cpuBankInfo.bankNumber and 0xFE)
        } else if (prgMode == 3) {
            addCpuMemoryMapping(0xA000, 0xBFFF, cpuBankInfo)
        }

        // Mode 0,1 - Ignored
        // Mode 2,3 - Select an 8KB PRG bank at $C000-$DFFF
        if (prgMode == 2 || prgMode == 3) {
            cpuBankInfo(0x5116, cpuBankInfo)
            addCpuMemoryMapping(0xC000, 0xDFFF, cpuBankInfo)
        }

        // Mode 0 - Select a 32KB PRG ROM bank at $8000-$FFFF (ignore bottom 2 bits)
        // Mode 1 - Select a 16KB PRG ROM bank at $C000-$FFFF (ignore bottom bit)
        // Mode 2,3 - Select an 8KB PRG ROM bank at $E000-$FFFF
        cpuBankInfo(0x5117, cpuBankInfo)

        when (prgMode) {
            0 -> addCpuMemoryMapping(0x8000, 0xFFFF, cpuBankInfo, cpuBankInfo.bankNumber and 0x7C)
            1 -> addCpuMemoryMapping(0xC000, 0xFFFF, cpuBankInfo, cpuBankInfo.bankNumber and 0x7E)
            2, 3 -> addCpuMemoryMapping(0xE000, 0xFFFF, cpuBankInfo, cpuBankInfo.bankNumber and 0x7F)
        }
    }

    private fun updateChrBanks(force: Boolean) {
        val largeSprites = mmc5MemoryHandler.readRegister(0x2000).bit5

        if (!largeSprites) {
            // Using 8x8 sprites resets the last written to bank logic.
            lastChrReg = 0
        }

        val chrA = !largeSprites ||
            splitTileNumber in 32..39 ||
            !ppuInFrame && lastChrReg <= 0x5127

        if (!force && chrA == prevChrA) {
            return
        }

        prevChrA = chrA

        when (chrMode) {
            0 -> selectChrPage8x(0, chrBanks[if (chrA) 0x07 else 0x0B] shl 3)
            1 -> {
                selectChrPage4x(0, chrBanks[if (chrA) 0x03 else 0x0B] shl 2)
                selectChrPage4x(1, chrBanks[if (chrA) 0x07 else 0x0B] shl 2)
            }
            2 -> {
                selectChrPage2x(0, chrBanks[if (chrA) 0x01 else 0x09] shl 1)
                selectChrPage2x(1, chrBanks[if (chrA) 0x03 else 0x0B] shl 1)
                selectChrPage2x(2, chrBanks[if (chrA) 0x05 else 0x09] shl 1)
                selectChrPage2x(3, chrBanks[if (chrA) 0x07 else 0x0B] shl 1)
            }
            else -> {
                selectChrPage(0, chrBanks[if (chrA) 0x00 else 0x08])
                selectChrPage(1, chrBanks[if (chrA) 0x01 else 0x09])
                selectChrPage(2, chrBanks[if (chrA) 0x02 else 0x0A])
                selectChrPage(3, chrBanks[if (chrA) 0x03 else 0x0B])
                selectChrPage(4, chrBanks[if (chrA) 0x04 else 0x08])
                selectChrPage(5, chrBanks[if (chrA) 0x05 else 0x09])
                selectChrPage(6, chrBanks[if (chrA) 0x06 else 0x0A])
                selectChrPage(7, chrBanks[if (chrA) 0x07 else 0x0B])
            }
        }
    }

    private fun switchChrBank(addr: Int, value: Int) {
        val newValue = value or (chrUpperBits shl 8)
        val reg = addr - 0x5120

        if (newValue != chrBanks[reg] || lastChrReg != addr) {
            chrBanks[reg] = newValue
            lastChrReg = addr
            updateChrBanks(true)
        }
    }

    override fun processCpuClock() {
        audio.clock()

        if (ppuIdleCounter > 0) {
            ppuIdleCounter--

            if (ppuIdleCounter == 0) {
                // The "in-frame" flag is cleared when the PPU is no longer rendering.
                // This is detected when 3 CPU cycles pass without a PPU read having
                // occurred (PPU /RD has not been low during the last 3 M2 rises).
                ppuInFrame = false
                updateChrBanks(true)
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun fillModeTile(tile: Int) {
        fillModeTile = tile
        fillNametable.fill(tile, 32 * 30) // 32 tiles per row, 30 rows
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun fillModeColor(color: Int) {
        fillModeColor = color
        val attributeByte = color or (color shl 2) or (color shl 4) or (color shl 6)
        fillNametable.fill(attributeByte, 64, 32 * 30) // Attribute table is 64 bytes.
    }

    private fun nametableMapping(value: Int) {
        nametableMapping = value

        repeat(4) {
            val nametableId = value shr (it * 2) and 0x03

            if (nametableId <= 1) {
                nametable(it, nametableId)
            } else if (nametableId == 2) {
                if (extendedRamMode <= 1) {
                    val source = if (hasBattery) Pointer(saveRam, mSaveRamSize - EXRAM_SIZE)
                    else Pointer(workRam, mWorkRamSize - EXRAM_SIZE)

                    addPpuMemoryMapping(0x2000 + it * 0x400, 0x2000 + it * 0x400 + 0x3FF, source, READ_WRITE)
                } else {
                    addPpuMemoryMapping(0x2000 + it * 0x400, 0x2000 + it * 0x400 + 0x3FF, emptyNametable, READ)
                }
            } else {
                addPpuMemoryMapping(0x2000 + it * 0x400, 0x2000 + it * 0x400 + 0x3FF, fillNametable, READ)
            }
        }
    }

    private fun extendedRamMode(mode: Int) {
        extendedRamMode = mode

        val accessType = when (mode) {
            // Mode 0/1 - Not readable (returns open bus), can only be written while
            // the PPU is rendering (otherwise, 0 is written)
            // See overridden write function for implementation.
            0, 1 -> WRITE
            // Mode 2 - Readable and writable.
            2 -> READ_WRITE
            // Mode 3 - Read-only.
            else -> READ
        }

        if (hasBattery) {
            addCpuMemoryMapping(0x5C00, 0x5FFF, SRAM, mSaveRamSize - EXRAM_SIZE, accessType)
        } else {
            addCpuMemoryMapping(0x5C00, 0x5FFF, WRAM, mWorkRamSize - EXRAM_SIZE, accessType)
        }

        nametableMapping(nametableMapping)
    }


    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        if (addr in 0x5C00..0x5FFF && extendedRamMode <= 1 && !ppuInFrame) {
            // Expansion RAM ($5C00-$5FFF, read/write)
            // Mode 0/1 - Not readable (returns open bus), can only be written while
            // the PPU is rendering (otherwise, 0 is written).
            super.write(addr, 0, type)
        } else {
            super.write(addr, value, type)
        }
    }

    private fun detectScanlineStart(addr: Int) {
        if (ntReadCounter >= 2) {
            // After 3 identical NT reads, trigger IRQ when the following attribute byte is read.

            if (!ppuInFrame && !needInFrame) {
                needInFrame = true
                scanlineCounter = 0
            } else {
                scanlineCounter++

                if (irqCounterTarget == scanlineCounter) {
                    irqPending = true

                    if (irqEnabled) {
                        console.cpu.setIRQSource(EXTERNAL)
                    }
                }
            }

            splitTileNumber = 0
            ntReadCounter = 0
        } else if (addr in 0x2000..0x2FFF) {
            if (lastPpuReadAddr == addr) {
                // Count consecutive identical reads.
                ntReadCounter++
            } else {
                ntReadCounter = 0
            }
        } else {
            ntReadCounter = 0
        }
    }

    override fun mapperReadVRAM(addr: Int): Int {
        val isNtFetch = addr in 0x2000..0x2FFF && addr and 0x3FF < 0x3C0

        if (isNtFetch) {
            // Nametable data, not an attribute fetch.
            splitInSplitRegion = false
            splitTileNumber++

            if (ppuInFrame) {
                updateChrBanks(false)
            } else if (needInFrame) {
                needInFrame = false
                ppuInFrame = true
                updateChrBanks(false)
            }
        }

        detectScanlineStart(addr)

        ppuIdleCounter = 3
        lastPpuReadAddr = addr

        if (extendedRamMode <= 1 && ppuInFrame) {
            if (verticalSplitEnabled) {
                val verticalSplitScroll = (verticalSplitScroll + scanlineCounter) % 240

                if (addr >= 0x2000) {
                    if (isNtFetch) {
                        val tileNumber = (splitTileNumber + 2) % 42

                        if (tileNumber <= 32 && (verticalSplitRightSide && tileNumber >= verticalSplitDelimiterTile || !verticalSplitRightSide && tileNumber < verticalSplitDelimiterTile)) {
                            // Split region (for next 3 fetches, attribute + 2x tile data).
                            splitInSplitRegion = true
                            splitTile = verticalSplitScroll and 0xF8 shl 2 or tileNumber
                            return internalRead(0x5C00 + splitTile)
                        } else {
                            // Outside of split region (or sprite data), result can get modified by ex ram mode code below.
                            splitInSplitRegion = false
                        }
                    } else if (splitInSplitRegion) {
                        return internalRead(0x5FC0 or (splitTile and 0x380 shr 4) or (splitTile and 0x1F shr 2))
                    }
                } else if (splitInSplitRegion) {
                    // CHR tile fetches for split region.
                    return chrRom[verticalSplitBank % (chrPageCount / 4) * 0x1000 + (addr and 0x07.inv() or (verticalSplitScroll and 0x07) and 0xFFF)]
                }
            }
            if (extendedRamMode == 1 && (splitTileNumber < 32 || splitTileNumber >= 40)) {
                // In Mode 1, nametable fetches are processed normally, and can come from CIRAM nametables, fill mode,
                // or even Expansion RAM, but attribute fetches are replaced by data from Expansion RAM.
                // Each byte of Expansion RAM is used to enhance the tile at the corresponding address in every nametable.

                // When fetching NT data, we set a flag and then alter the VRAM values read by
                // the PPU on the following 3 cycles (palette, tile low/high byte).
                if (isNtFetch) {
                    // Nametable fetches.
                    exAttributeLastNametableFetch = addr and 0x03FF
                    exAttrLastFetchCounter = 3
                } else if (exAttrLastFetchCounter > 0) {
                    // Attribute fetches.
                    exAttrLastFetchCounter--

                    when (exAttrLastFetchCounter) {
                        2 -> {
                            // PPU palette fetch
                            // Check work ram (expansion ram) to see which tile/palette to use
                            // Use internalRead to bypass the fact that the ram is supposed to be write-only in mode 0/1
                            val value = internalRead(0x5C00 + exAttributeLastNametableFetch)

                            // The pattern fetches ignore the standard CHR banking bits, and instead use the top two bits of $5130 and the
                            // bottom 6 bits from Expansion RAM to choose a 4KB bank to select the tile from.
                            exAttrSelectedChrBank = (value and 0x3F or (chrUpperBits shl 6)) % (mChrRomSize / 0x1000)

                            // Return a byte containing the same palette 4 times - this allows the PPU to select
                            // the right palette no matter the shift value
                            val palette = value and 0xC0 shr 6

                            return palette or (palette shl 2) or (palette shl 4) or (palette shl 6)
                        }
                        // PPU tile data fetch (high byte & low byte).
                        1, 0 -> return chrRom[exAttrSelectedChrBank * 0x1000 + (addr and 0xFFF)]
                    }
                }
            }
        }

        return internalReadVRAM(addr)
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr) {
            in 0x5113..0x5117 -> switchPrgBank(addr, value)
            in 0x5120..0x512B -> switchChrBank(addr, value)
            else -> when (addr) {
                0x5000, 0x5001, 0x5002, 0x5003, 0x5004, 0x5005,
                0x5006, 0x5007, 0x5010, 0x5011, 0x5015 -> audio.write(addr, value)
                0x5100 -> {
                    prgMode = value and 0x03
                    updatePrgBanks()
                }
                0x5101 -> {
                    chrMode = value and 0x03
                    updateChrBanks(true)
                }
                0x5102 -> {
                    prgRamProtect1 = value and 0x03
                    updatePrgBanks()
                }
                0x5103 -> {
                    prgRamProtect2 = value and 0x03
                    updatePrgBanks()
                }
                0x5104 -> extendedRamMode(value and 0x03)
                0x5105 -> nametableMapping(value)
                0x5106 -> fillModeTile(value)
                0x5107 -> fillModeColor(value and 0x03)
                0x5130 -> chrUpperBits = value and 0x03
                0x5200 -> {
                    verticalSplitEnabled = value.bit7
                    verticalSplitRightSide = value.bit6
                    verticalSplitDelimiterTile = value and 0x1F
                }
                0x5201 -> verticalSplitScroll = value
                0x5202 -> verticalSplitBank = value
                0x5203 -> irqCounterTarget = value
                0x5204 -> {
                    irqEnabled = value.bit7

                    if (!irqEnabled) {
                        console.cpu.clearIRQSource(EXTERNAL)
                    } else if (irqPending) {
                        console.cpu.setIRQSource(EXTERNAL)
                    }
                }
                0x5205 -> multiplierValue1 = value
                0x5206 -> multiplierValue2 = value
                else -> Unit
            }
        }
    }

    override fun readRegister(addr: Int): Int {
        when (addr) {
            0x5010, 0x5015 -> return audio.read(addr)
            0x5204 -> {
                val value = (if (ppuInFrame) 0x40 else 0x00) or if (irqPending) 0x80 else 0x00
                irqPending = false
                console.cpu.clearIRQSource(EXTERNAL)
                return value
            }
            0x5205 -> return multiplierValue1 * multiplierValue2 and 0xFF
            0x5206 -> return multiplierValue1 * multiplierValue2 shr 8
            0xFFFA, 0xFFFB -> {
                ppuInFrame = false
                updateChrBanks(true)
                lastPpuReadAddr = 0
                scanlineCounter = 0
                irqPending = false
                console.cpu.clearIRQSource(EXTERNAL)
                return debugRead(addr)
            }
        }

        return console.memoryManager.openBus()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgBanks", prgBanks)
        s.write("chrBanks", chrBanks)
        s.write("audio", audio)
        s.write("prgRamProtect1", prgRamProtect1)
        s.write("prgRamProtect2", prgRamProtect2)
        s.write("fillModeTile", fillModeTile)
        s.write("fillModeColor", fillModeColor)
        s.write("verticalSplitEnabled", verticalSplitEnabled)
        s.write("verticalSplitRightSide", verticalSplitRightSide)
        s.write("verticalSplitDelimiterTile", verticalSplitDelimiterTile)
        s.write("verticalSplitScroll", verticalSplitScroll)
        s.write("verticalSplitBank", verticalSplitBank)
        s.write("multiplierValue1", multiplierValue1)
        s.write("multiplierValue2", multiplierValue2)
        s.write("nametableMapping", nametableMapping)
        s.write("extendedRamMode", extendedRamMode)
        s.write("exAttributeLastNametableFetch", exAttributeLastNametableFetch)
        s.write("exAttrLastFetchCounter", exAttrLastFetchCounter)
        s.write("exAttrSelectedChrBank", exAttrSelectedChrBank)
        s.write("prgMode", prgMode)
        s.write("chrMode", chrMode)
        s.write("chrUpperBits", chrUpperBits)
        s.write("lastChrReg", lastChrReg)
        s.write("irqCounterTarget", irqCounterTarget)
        s.write("irqEnabled", irqEnabled)
        s.write("scanlineCounter", scanlineCounter)
        s.write("irqPending", irqPending)
        s.write("ppuInFrame", ppuInFrame)
        s.write("splitInSplitRegion", splitInSplitRegion)
        s.write("splitVerticalScroll", splitVerticalScroll)
        s.write("splitTile", splitTile)
        s.write("splitTileNumber", splitTileNumber)
        s.write("needInFrame", needInFrame)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("prgBanks", prgBanks)
        s.readIntArray("chrBanks", chrBanks)
        s.readSnapshotable("audio", audio)
        prgRamProtect1 = s.readInt("prgRamProtect1")
        prgRamProtect2 = s.readInt("prgRamProtect2")
        fillModeTile = s.readInt("fillModeTile")
        fillModeColor = s.readInt("fillModeColor")
        verticalSplitEnabled = s.readBoolean("verticalSplitEnabled")
        verticalSplitRightSide = s.readBoolean("verticalSplitRightSide")
        verticalSplitDelimiterTile = s.readInt("verticalSplitDelimiterTile")
        verticalSplitScroll = s.readInt("verticalSplitScroll")
        verticalSplitBank = s.readInt("verticalSplitBank")
        multiplierValue1 = s.readInt("multiplierValue1")
        multiplierValue2 = s.readInt("multiplierValue2")
        nametableMapping = s.readInt("nametableMapping")
        extendedRamMode = s.readInt("extendedRamMode")
        exAttributeLastNametableFetch = s.readInt("exAttributeLastNametableFetch")
        exAttrLastFetchCounter = s.readInt("exAttrLastFetchCounter")
        exAttrSelectedChrBank = s.readInt("exAttrSelectedChrBank")
        prgMode = s.readInt("prgMode")
        chrMode = s.readInt("chrMode")
        chrUpperBits = s.readInt("chrUpperBits")
        lastChrReg = s.readInt("lastChrReg")
        irqCounterTarget = s.readInt("irqCounterTarget")
        irqEnabled = s.readBoolean("irqEnabled")
        scanlineCounter = s.readInt("scanlineCounter")
        irqPending = s.readBoolean("irqPending")
        ppuInFrame = s.readBoolean("ppuInFrame")
        splitInSplitRegion = s.readBoolean("splitInSplitRegion")
        splitVerticalScroll = s.readInt("splitVerticalScroll")
        splitTile = s.readInt("splitTile")
        splitTileNumber = s.readInt("splitTileNumber", -1)
        needInFrame = s.readBoolean("needInFrame")

        updatePrgBanks()
        fillModeTile(fillModeTile)
        fillModeColor(fillModeColor)
        nametableMapping(nametableMapping)
    }

    companion object {

        const val EXRAM_SIZE = 0x400
    }
}
