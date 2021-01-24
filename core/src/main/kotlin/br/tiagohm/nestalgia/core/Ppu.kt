package br.tiagohm.nestalgia.core

import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
open class Ppu(val console: Console) :
    MemoryHandler,
    Resetable,
    Snapshotable {

    private val settings = console.settings
    private var standardVBlankEnd = 0
    private var standardNmiScanline = 0
    private var vBlankEnd = 0
    private var nmiScanline = 0
    private var palSpriteEvalScanline = 0

    private var masterClock = 0L
    private var masterClockDivider = 4
    private var memoryReadBuffer: UByte = 0U

    private val outputBuffers = arrayOf(
        UShortArray(PIXEL_COUNT),
        UShortArray(PIXEL_COUNT),
    )

    private var currentOutputBuffer = outputBuffers[0]

    val paletteRAM = ubyteArrayOf(
        0x09U, 0x01U, 0x00U, 0x01U, 0x00U, 0x02U, 0x02U, 0x0DU, //
        0x08U, 0x10U, 0x08U, 0x24U, 0x00U, 0x00U, 0x04U, 0x2CU, //
        0x09U, 0x01U, 0x34U, 0x03U, 0x00U, 0x04U, 0x00U, 0x14U, //
        0x08U, 0x3AU, 0x00U, 0x02U, 0x00U, 0x20U, 0x2CU, 0x08U, //
    )

    val spriteRAM = UByteArray(0x100)
    val secondarySpriteRAM = UByteArray(0x20)
    private val hasSprite = BooleanArray(257)

    private var spriteCount = 0U
    private var secondaryOAMAddr = 0U
    private var sprite0Visible = false

    private var firstVisibleSpriteAddr: UByte = 0U
    private var lastVisibleSpriteAddr: UByte = 0U
    private var spriteIndex = 0U

    private var intensifyColorBits: UShort = 0U
    private var paletteRamMask: UByte = 0U
    private var lastUpdatedPixel = 0

    private var lastSprite: SpriteInfo? = null

    val spriteTiles = Array(64) { SpriteInfo() }

    private val openBusDecayStamp = IntArray(8)
    private var ignoreVramRead = 0U

    private var oamCopybuffer: UByte = 0U
    private var spriteInRange = false
    private var sprite0Added = false
    private var spriteAddrH: UByte = 0U
    private var spriteAddrL: UByte = 0U
    private var oamCopyDone = false
    private var overflowBugCounter: UByte = 0U

    @PublishedApi
    internal var ppuBusAddress: UShort = 0U

    private var isNeedStateUpdate = false
    private var isRenderingEnabled = false
    private var prevIsRenderingEnabled = false
    private var preventVBlankFlag = false

    val oamDecayCycles = LongArray(0x40)
    val corruptOamRow = BooleanArray(32)

    var enableOamDecay = false
        private set

    private var updateVramAddr: UShort = 0U
    private var updateVramAddrDelay: UByte = 0U

    @PublishedApi
    internal var state = PpuState()

    private var flags = PpuControl()
    private var statusFlags = PpuStatus()

    private var minimumDrawBgCycle = 0U
    private var minimumDrawSpriteCycle = 0U
    private var minimumDrawSpriteStandardCycle = 0U

    private var currentTile = TileInfo()
    private var nextTile = TileInfo()
    private var previousTile = TileInfo()

    var scanline = 0
        private set

    var cycle = 0
        private set

    var frameCount = 0
        private set

    inline val frameCycle: Int
        get() = (scanline + 1) * 341 + cycle

    var openBus: UByte = 0U
        private set

    init {
        console.initializeRam(spriteRAM)
        console.initializeRam(secondarySpriteRAM)

        reset(false)
    }

    override fun reset(softReset: Boolean) {
        masterClock = 0L
        preventVBlankFlag = false

        isNeedStateUpdate = false
        prevIsRenderingEnabled = false
        isRenderingEnabled = false

        ignoreVramRead = 0U
        openBus = 0U

        openBusDecayStamp.fill(0)

        state = PpuState()
        flags = PpuControl()
        statusFlags = PpuStatus()

        previousTile = TileInfo()
        currentTile = TileInfo()
        nextTile = TileInfo()

        ppuBusAddress = 0U
        intensifyColorBits = 0U
        paletteRamMask = 0x3FU
        lastUpdatedPixel = -1
        lastSprite = null
        oamCopybuffer = 0U
        spriteInRange = false
        sprite0Added = false
        spriteAddrH = 0U
        spriteAddrL = 0U
        oamCopyDone = false

        hasSprite.fill(false)
        spriteTiles.indices.forEach { i -> spriteTiles[i] = SpriteInfo() }

        spriteCount = 0U
        secondaryOAMAddr = 0U
        sprite0Visible = false
        spriteIndex = 0U
        openBus = 0U

        ignoreVramRead = 0U

        // First execution will be cycle 0, scanline 0
        scanline = -1
        cycle = 340

        frameCount = 1
        memoryReadBuffer = 0U

        overflowBugCounter = 0U

        updateVramAddrDelay = 0U
        updateVramAddr = 0U

        oamDecayCycles.fill(0)
        enableOamDecay = settings.checkFlag(EmulationFlag.ENABLE_OAM_DECAY)

        updateMinimumDrawCycles()
    }

    private inline fun updateMinimumDrawCycles() {
        minimumDrawBgCycle =
            if (flags.backgroundEnabled) if (flags.backgroundMask || settings.checkFlag(EmulationFlag.FORCE_BACKGROUND_FIRST_COLUMN)) 0U else 8U else 300U
        minimumDrawSpriteCycle =
            if (flags.spritesEnabled) if (flags.spriteMask || settings.checkFlag(EmulationFlag.FORCE_SPRITES_FIRST_COLUMN)) 0U else 8U else 300U
        minimumDrawSpriteStandardCycle = if (flags.spritesEnabled) if (flags.spriteMask) 0U else 8U else 300U
    }

    var region: Region = Region.AUTO
        set(value) {
            field = value

            when (value) {
                Region.AUTO -> {
                    throw IllegalArgumentException("Should never be AUTO")
                }
                Region.NTSC -> {
                    nmiScanline = 241
                    vBlankEnd = 260
                    standardNmiScanline = 241
                    standardVBlankEnd = 260
                    masterClockDivider = 4
                }
                Region.PAL -> {
                    nmiScanline = 241
                    vBlankEnd = 310
                    standardNmiScanline = 241
                    standardVBlankEnd = 310
                    masterClockDivider = 5
                }
                Region.DENDY -> {
                    nmiScanline = 291
                    vBlankEnd = 310
                    standardNmiScanline = 291
                    standardVBlankEnd = 310
                    masterClockDivider = 5
                }
            }

            nmiScanline += settings.extraScanlinesBeforeNmi
            palSpriteEvalScanline = nmiScanline + 24
            standardVBlankEnd += settings.extraScanlinesBeforeNmi
            vBlankEnd += settings.extraScanlinesAfterNmi + settings.extraScanlinesBeforeNmi
        }

    val overclockRate: Double
        get() {
            return (vBlankEnd.toDouble() + 2) / (2 + when (region) {
                Region.PAL -> 310
                Region.DENDY -> 310
                else -> 260
            })
        }

    fun run(runTo: Long) {
        while (masterClock + masterClockDivider <= runTo) {
            exec()
            masterClock += masterClockDivider
        }
    }

    // "inline" turns it over slow. Why?
    private fun exec() {
        if (cycle > 339) {
            cycle = 0

            if (++scanline > vBlankEnd) {
                lastUpdatedPixel = -1
                scanline = -1

                // Force prerender scanline sprite fetches to load the dummy $FF tiles (fixes shaking in Ninja Gaiden 3 stage 1 after beating boss)
                spriteCount = 0U

                if (isRenderingEnabled) {
                    processOamCorruption()
                }

                updateMinimumDrawCycles()
            }

            processPpuCycle()
            updateApuStatus()

            if (scanline == settings.inputPollScanline) {
                console.controlManager.updateInputState()
            }

            if (scanline == -1) {
                statusFlags.spriteOverflow = false
                statusFlags.sprite0Hit = false

                // Switch to alternate output buffer (VideoDecoder may still be decoding the last frame buffer)
                currentOutputBuffer = if (currentOutputBuffer == outputBuffers[0]) {
                    outputBuffers[1]
                } else {
                    outputBuffers[0]
                }
            } else if (scanline == 240) {
                // At the start of vblank, the bus address is set back to VideoRamAddr.
                // According to Visual NES, this occurs on scanline 240, cycle 1, but is done here on cycle for performance reasons
                setBusAddress(state.videoRamAddr)
                sendFrame()
                frameCount++
            }
        } else {
            cycle++

            processPpuCycle()

            if (scanline < 240) {
                processScanline()
            } else if (cycle == 1 && scanline == nmiScanline) {
                if (!preventVBlankFlag) {
                    statusFlags.verticalBlank = true
                    beginVBlank()
                }

                preventVBlankFlag = false
            } else if (region == Region.PAL && scanline >= palSpriteEvalScanline) {
                // On a PAL machine, because of its extended vertical blank, the PPU begins refreshing OAM roughly 21 scanlines after NMI[2], to prevent it
                // from decaying during the longer hiatus of rendering. Additionally, it will continue to refresh during the visible portion of the screen
                // even if rendering is disabled. Because of this, OAM DMA must be done near the beginning of vertical blank on PAL, and everywhere else
                // it is liable to conflict with the refresh. Since the refresh can't be disabled like on the NTSC hardware, OAM decay does not occur at all on the PAL NES.
                if (cycle <= 256) {
                    processSpriteEvaluation()
                } else if (cycle in 257..319) {
                    state.spriteRamAddr = 0U
                }
            }
        }

        if (isNeedStateUpdate) {
            updateState()
        }
    }

    private inline fun updateApuStatus() {
        val apu = console.apu

        apu.isEnabled = true

        if (scanline > 240) {
            if (scanline > standardVBlankEnd) {
                // Disable APU for extra lines after NMI
                apu.isEnabled = false
            } else if (scanline in standardNmiScanline until nmiScanline) {
                // Disable APU for extra lines before NMI
                apu.isEnabled = false
            }
        }
    }

    private inline fun beginVBlank() {
        triggerNmi()
    }

    private inline fun triggerNmi() {
        if (flags.vBlank) {
            console.cpu.nmi = true
        }
    }

    private fun sendFrame() {
        updateGrayscaleAndIntensifyBits()

        // If VideoDecoder isn't done with the previous frame,
        // updateFrame will block until it is ready to accept a new frame.
        console.videoDecoder.updateFrame(currentOutputBuffer)

        enableOamDecay = settings.checkFlag(EmulationFlag.ENABLE_OAM_DECAY)
    }

    private inline fun updateGrayscaleAndIntensifyBits() {
        if (scanline < 0 || scanline > nmiScanline) {
            return
        }

        val pixelNumber = when {
            scanline >= 240 -> {
                61439
            }
            cycle < 3 -> {
                (scanline shl 8) - 1
            }
            cycle <= 258 -> {
                (scanline shl 8) + cycle - 3
            }
            else -> {
                (scanline shl 8) + 255
            }
        }

        if (paletteRamMask.toUInt() == 0x3FU && intensifyColorBits.isZero) {
            // Nothing to do (most common case)
            lastUpdatedPixel = pixelNumber
            return
        }

        if (lastUpdatedPixel < pixelNumber) {
            var out = lastUpdatedPixel + 1

            while (lastUpdatedPixel < pixelNumber) {
                currentOutputBuffer[out] =
                    (currentOutputBuffer[out] and paletteRamMask.toUShort()) or intensifyColorBits
                out++
                lastUpdatedPixel++
            }
        }
    }

    private inline fun processScanline() {
        // Only called for cycle 1+
        if (cycle <= 256) {
            loadTileInfo()

            if (prevIsRenderingEnabled && (cycle and 0x07) == 0) {
                incHorizontalScrolling()

                if (cycle == 256) {
                    incVerticalScrolling()
                }
            }

            if (scanline >= 0) {
                drawPixel()
                shiftTileRegisters()
                // Secondary OAM clear and sprite evaluation do not occur on the pre-render line
                processSpriteEvaluation()
            } else if (cycle < 9) {
                // Pre-render scanline logic
                if (cycle == 1) {
                    statusFlags.verticalBlank = false
                    console.cpu.nmi = false
                }

                if (state.spriteRamAddr >= 0x08U && isRenderingEnabled && !settings.checkFlag(EmulationFlag.DISABLE_OAM_ADDR_BUG)) {
                    // This should only be done if rendering is enabled (otherwise oam_stress test fails immediately)
                    // If OAMADDR is not less than eight when rendering starts, the eight bytes starting at OAMADDR & 0xF8 are copied to the first eight bytes of OAM
                    writeSpriteRam(
                        (cycle - 1).toUByte(),
                        readSpriteRam(((state.spriteRamAddr and 0xF8U) + cycle.toUInt() - 1U).toUByte())
                    )
                }
            }
        } else if (cycle in 257..320) {
            if (cycle == 257) {
                spriteIndex = 0U
                hasSprite.fill(false)

                if (prevIsRenderingEnabled) {
                    // copy horizontal scrolling value from t
                    // _state.VideoRamAddr = (_state.VideoRamAddr & ~0x041F) | (_state.TmpVideoRamAddr & 0x041F)
                    state.videoRamAddr = (state.videoRamAddr and 0xFBE0U) or (state.tmpVideoRamAddr and 0x041FU)
                }
            }

            if (isRenderingEnabled) {
                // OAMADDR is set to 0 during each of ticks 257-320 (the sprite tile loading interval) of the pre-render and visible scanlines. (When rendering)
                state.spriteRamAddr = 0U

                when {
                    (cycle - 261) % 8 == 0 -> {
                        // Cycle 260, 268, etc. This is an approximation (each tile is actually loaded in 8 steps (e.g from 257 to 264))
                        loadSpriteTileInfo()
                    }
                    (cycle - 257) % 8 == 0 -> {
                        // Garbage NT sprite fetch (257, 265, 273, etc.) - Required for proper MC-ACC IRQs (MMC3 clone)
                        readVRam(nametableAddress)
                    }
                    (cycle - 259) % 8 == 0 -> {
                        // Garbage AT sprite fetch
                        readVRam(attributeAddress)
                    }
                }

                if (scanline == -1 && cycle >= 280 && cycle <= 304) {
                    // copy vertical scrolling value from t
                    // _state.VideoRamAddr = (_state.VideoRamAddr & ~0x7BE0) | (_state.TmpVideoRamAddr & 0x7BE0);
                    state.videoRamAddr = (state.videoRamAddr and 0x841FU) or (state.tmpVideoRamAddr and 0x7BE0U)
                }
            }
        } else if (cycle in 321..336) {
            if (cycle == 321) {
                if (isRenderingEnabled) {
                    loadExtraSprites()
                    oamCopybuffer = secondarySpriteRAM[0]
                }

                loadTileInfo()
            } else if (prevIsRenderingEnabled && (cycle == 328 || cycle == 336)) {
                loadTileInfo()
                state.lowBitShift = (state.lowBitShift.toUInt() shl 8).toUShort()
                state.highBitShift = (state.highBitShift.toUInt() shl 8).toUShort()
                incHorizontalScrolling()
            } else {
                loadTileInfo()
            }
        } else if (cycle == 337 || cycle == 339) {
            if (isRenderingEnabled) {
                readVRam(nametableAddress)

                if (scanline == -1 &&
                    cycle == 339 &&
                    (frameCount and 0x01) == 0x01 &&
                    region == Region.NTSC &&
                    settings.ppuModel == PpuModel.PPU_2C02
                ) {
                    // This behavior is NTSC-specific - PAL frames are always the same number of cycles
                    // With rendering enabled, each odd PPU frame is one PPU clock shorter than normal" (skip from 339 to 0, going over 340)
                    cycle = 340
                }
            }
        }
    }

    private inline fun loadSpriteTileInfo() {
        val a = (spriteIndex * 4U).toInt()

        loadSprite(
            secondarySpriteRAM[a],
            secondarySpriteRAM[a + 1],
            secondarySpriteRAM[a + 2],
            secondarySpriteRAM[a + 3],
            false
        )
    }

    private fun loadExtraSprites() {
        if (spriteCount == 8U && settings.checkFlag(EmulationFlag.REMOVE_SPRITE_LIMIT)) {
            var loadExtraSprites = true

            if (settings.checkFlag(EmulationFlag.ADAPTIVE_SPRITE_LIMIT)) {
                var lastPosition = -1
                var identicalSpriteCount: UByte = 0U
                var maxIdenticalSpriteCount: UByte = 0U

                for (i in 0..63) {
                    val y = spriteRAM[i shl 2].toInt()

                    if (scanline >= y &&
                        scanline < y + (if (flags.largeSprites) 16 else 8)
                    ) {
                        val x = spriteRAM[(i shl 2) + 3]
                        val position = (y shl 8) or x.toInt()

                        if (lastPosition != position) {
                            if (identicalSpriteCount > maxIdenticalSpriteCount) {
                                maxIdenticalSpriteCount = identicalSpriteCount
                            }

                            lastPosition = position
                            identicalSpriteCount = 1U
                        } else {
                            identicalSpriteCount++
                        }
                    }
                }

                loadExtraSprites = identicalSpriteCount < 8U && maxIdenticalSpriteCount < 8U
            }

            if (loadExtraSprites) {
                var i = (lastVisibleSpriteAddr.toInt() + 4) and 0xFF

                while (i != firstVisibleSpriteAddr.toInt()) {
                    val spriteY = spriteRAM[i]

                    if (scanline >= spriteY.toInt() &&
                        scanline < spriteY.toInt() + (if (flags.largeSprites) 16 else 8)
                    ) {
                        loadSprite(
                            spriteY,
                            spriteRAM[i + 1],
                            spriteRAM[i + 2],
                            spriteRAM[i + 3],
                            true
                        )

                        spriteCount++
                    }

                    i = (i + 4) and 0xFF
                }
            }
        }
    }

    private inline fun drawPixel() {
        // This is called 3.7 million times per second - needs to be as fast as possible.
        if (isRenderingEnabled || (state.videoRamAddr.toUInt() and 0x3F00U) != 0x3F00U) {
            val color = getPixelColor()
            currentOutputBuffer[(scanline shl 8) + cycle - 1] =
                paletteRAM[if (color and 0x03 != 0) color else 0].toUShort()
        } else {
            // If the current VRAM address points in the range $3F00-$3FFF during forced blanking,
            // the color indicated by this palette location will be shown on screen instead of the backdrop color.
            currentOutputBuffer[(scanline shl 8) + cycle - 1] =
                paletteRAM[(state.videoRamAddr and 0x1FU).toInt()].toUShort()
        }
    }

    fun getCurrentBgColor(): UShort {
        val color = if (isRenderingEnabled || (state.videoRamAddr.toUInt() and 0x3F00U) != 0x3F00U) {
            paletteRAM[0].toUShort()
        } else {
            paletteRAM[(state.videoRamAddr and 0x1FU).toInt()].toUShort()
        }

        return (color and paletteRamMask.toUShort()) or intensifyColorBits
    }

    private fun getPixelColor(): Int {
        val offset = state.xScroll
        var backgroundColor: UByte = 0U
        var spriteBgColor: UByte = 0U

        if (cycle > minimumDrawBgCycle.toInt()) {
            // BackgroundMask = false: Hide background in leftmost 8 pixels of screen
            spriteBgColor =
                ((((state.lowBitShift.toUInt() shl offset.toInt()) and 0x8000U) shr 15) or (((state.highBitShift.toUInt() shl offset.toInt()) and 0x8000U) shr 14)).toUByte()

            if (settings.isBackgroundEnabled) {
                backgroundColor = spriteBgColor
            }
        }

        if (hasSprite[cycle] && cycle > minimumDrawSpriteCycle.toInt()) {
            for (i in 0 until spriteCount.toInt()) {
                val shift = cycle - spriteTiles[i].spriteX.toInt() - 1

                if (shift in 0..7) {
                    lastSprite = spriteTiles[i]

                    val spriteColor = if (spriteTiles[i].horizontalMirror) {
                        ((lastSprite!!.lowByte shr shift) and 0x01U) or (((lastSprite!!.highByte shr shift) and 0x01U).toUInt() shl 1).toUByte()
                    } else {
                        (((lastSprite!!.lowByte.toUInt() shl shift) and 0x80U) shr 7).toUByte() or (((lastSprite!!.highByte.toUInt() shl shift) and 0x80U) shr 6).toUByte()
                    }

                    if (spriteColor.isNonZero) {
                        // First sprite without a 00 color, use it.
                        if (i == 0 &&
                            spriteBgColor.isNonZero &&
                            sprite0Visible &&
                            cycle != 256 &&
                            flags.backgroundEnabled &&
                            !statusFlags.sprite0Hit &&
                            cycle > minimumDrawSpriteStandardCycle.toInt()
                        ) {
                            // The hit condition is basically sprite zero is in range AND the first sprite output unit is outputting a non-zero pixel AND the background drawing unit is outputting a non-zero pixel.
                            // Sprite zero hits do not register at x=255 (cycle 256)
                            // ...provided that background and sprite rendering are both enabled
                            // Should always miss when Y >= 239
                            statusFlags.sprite0Hit = true
                        }

                        if (settings.spritesEnabled &&
                            (backgroundColor.isZero || !spriteTiles[i].backgroundPriority)
                        ) {
                            // Check sprite priority
                            return (lastSprite!!.paletteOffset + spriteColor).toInt()
                        }

                        break
                    }
                }
            }
        }

        return ((if (offset.toInt() + ((cycle - 1) and 0x07) < 8) previousTile else currentTile).paletteOffset + backgroundColor).toInt()
    }

    private inline fun shiftTileRegisters() {
        state.lowBitShift = (state.lowBitShift.toUInt() shl 1).toUShort()
        state.highBitShift = (state.highBitShift.toUInt() shl 1).toUShort()
    }

    private fun loadSprite(
        spriteY: UByte,
        tileIndex: UByte,
        attributes: UByte,
        spriteX: UByte,
        extraSprite: Boolean
    ) {
        var ti = tileIndex
        val backgroundPriority = attributes.bit5
        val horizontalMirror = attributes.bit6
        val verticalMirror = attributes.bit7

        var lineOffset = if (verticalMirror) {
            ((if (flags.largeSprites) 15 else 7) - (scanline - spriteY.toInt())).toUByte()
        } else {
            (scanline - spriteY.toInt()).toUByte()
        }

        var tileAddr = if (flags.largeSprites) {
            (((if (ti.bit0) 0x1000U else 0x0000U) or ((ti.toUInt() and 0xFEU) shl 4)) + (if (lineOffset >= 8U) lineOffset + 8U else lineOffset.toUInt())).toUShort()
        } else {
            (((ti.toUInt() shl 4) or flags.spritePatternAddr.toUInt()) + lineOffset).toUShort()
        }

        var fetchLastSprite = true

        if ((spriteIndex < spriteCount || extraSprite) && spriteY < 240U) {
            val info = spriteTiles[spriteIndex.toInt()]

            info.backgroundPriority = backgroundPriority
            info.horizontalMirror = horizontalMirror
            info.verticalMirror = verticalMirror
            info.paletteOffset = ((attributes and 0x03U).toUInt() shl 2) or 0x10U

            if (extraSprite) {
                // Use DebugReadVRAM for extra sprites to prevent side-effects.
                info.lowByte = console.mapper!!.debugReadVRAM(tileAddr)
                info.highByte = console.mapper!!.debugReadVRAM((tileAddr + 8U).toUShort())
            } else {
                fetchLastSprite = false
                info.lowByte = readVRam(tileAddr)
                info.highByte = readVRam((tileAddr + 8U).toUShort())
            }

            info.tileAddr = tileAddr
            // info.AbsoluteTileAddr = _console->GetMapper()->ToAbsoluteChrAddress(tileAddr);
            // info.offsetY = lineOffset
            info.spriteX = spriteX

            if (scanline >= 0) {
                // Sprites read on prerender scanline are not shown on scanline 0
                var i = 0
                while (i < 8 && (spriteX.toInt() + i + 1) < 257) {
                    hasSprite[spriteX.toInt() + i + 1] = true
                    i++
                }
            }
        }

        if (fetchLastSprite) {
            // Fetches to sprite 0xFF for remaining sprites/hidden - used by MMC3 IRQ counter
            lineOffset = 0U
            ti = 0xFFU

            tileAddr = if (flags.largeSprites) {
                (((if (ti.bit0) 0x1000U else 0x0000U) or ((ti.toUInt() and 0xFFFEU) shl 4)) + (if (lineOffset >= 8U) lineOffset + 8U else lineOffset.toUInt())).toUShort()
            } else {
                (((ti.toUInt() shl 4) or flags.spritePatternAddr.toUInt()) + lineOffset).toUShort()
            }

            readVRam(tileAddr)
            readVRam((tileAddr + 8U).toUShort())
        }

        spriteIndex++
    }

    private fun loadTileInfo() {
        if (isRenderingEnabled) {
            when (cycle and 0x07) {
                1 -> {
                    previousTile.copyFrom(currentTile)
                    currentTile.copyFrom(nextTile)

                    state.lowBitShift = state.lowBitShift or nextTile.lowByte.toUShort()
                    state.highBitShift = state.highBitShift or nextTile.highByte.toUShort()

                    val tileIndex = readVRam(nametableAddress)
                    nextTile.tileAddr =
                        ((tileIndex.toUInt() shl 4) or (state.videoRamAddr.toUInt() shr 12) or flags.backgroundPatternAddr.toUInt()).toUShort()
                    nextTile.offsetY = (state.videoRamAddr shr 12).toUByte()
                }
                3 -> {
                    val shift = (((state.videoRamAddr shr 4) and 0x04U) or (state.videoRamAddr and 0x02U)).toInt()
                    nextTile.paletteOffset = ((readVRam(attributeAddress).toUInt() shr shift) and 0x03U) shl 2
                }
                5 -> {
                    nextTile.lowByte = readVRam(nextTile.tileAddr)
                    // TODO: HD PPU nextTile.AbsoluteTileAddr = console.mapper.toAbsoluteChrAddress(nextTile.tileAddr)
                }
                7 -> {
                    nextTile.highByte = readVRam((nextTile.tileAddr + 8U).toUShort())
                }
            }
        }
    }

    private fun updateState() {
        isNeedStateUpdate = false

        // Rendering enabled flag is apparently set with a 1 cycle delay (i.e setting it at cycle 5 will render cycle 6 like cycle 5 and then take the new settings for cycle 7)
        if (prevIsRenderingEnabled != isRenderingEnabled) {
            prevIsRenderingEnabled = isRenderingEnabled

            if (scanline < 240) {
                if (prevIsRenderingEnabled) {
                    // Rendering was just enabled, perform oam corruption if any is pending
                    processOamCorruption()
                } else {
                    // Rendering was just disabled by a write to $2001, check for oam row corruption glitch
                    setOamCorruptionFlags()

                    // When rendering is disabled midscreen, set the vram bus back to the value of 'v'
                    setBusAddress(state.videoRamAddr and 0x3FFFU)

                    if (cycle in 65..256) {
                        // Disabling rendering during OAM evaluation will trigger a glitch causing the current address to be incremented by 1
                        // The increment can be "delayed" by 1 PPU cycle depending on whether or not rendering is disabled on an even/odd cycle
                        // e.g, if rendering is disabled on an even cycle, the following PPU cycle will increment the address by 5 (instead of 4)
                        // if rendering is disabled on an odd cycle, the increment will wait until the next odd cycle (at which point it will be incremented by 1)
                        // In practice, there is no way to see the difference, so we just increment by 1 at the end of the next cycle after rendering was disabled
                        state.spriteRamAddr++

                        // Also corrupt H/L to replicate a bug found in oam_flicker_test_reenable when rendering is disabled around scanlines 128-136
                        // Reenabling the causes the OAM evaluation to restart misaligned, and ends up generating a single sprite that's offset by 1
                        // such that it's Y=tile index, index = attributes, attributes = x, and X = the next sprite's Y value
                        spriteAddrH = ((state.spriteRamAddr shr 2) and 0x3FU).toUByte()
                        spriteAddrL = (state.spriteRamAddr and 0x03U).toUByte()
                    }
                }
            }
        }

        if (isRenderingEnabled != (flags.backgroundEnabled or flags.spritesEnabled)) {
            isRenderingEnabled = flags.backgroundEnabled or flags.spritesEnabled
            isNeedStateUpdate = true
        }

        if (updateVramAddrDelay.isNonZero) {
            updateVramAddrDelay--

            if (updateVramAddrDelay.isZero) {
                if (settings.checkFlag(EmulationFlag.ENABLE_PPU_2006_SCROLL_GLITCH) &&
                    scanline < 240 &&
                    isRenderingEnabled
                ) {
                    //When a $2006 address update lands on the Y or X increment, the written value is bugged and is ANDed with the incremented value
                    if (cycle == 257) {
                        state.videoRamAddr = state.videoRamAddr and updateVramAddr
                    } else if (cycle > 0 && (cycle and 0x07) == 0 && (cycle <= 256 || cycle > 320)) {
                        // _state.VideoRamAddr = (_updateVramAddr & ~0x41F) | (_state.VideoRamAddr & _updateVramAddr & 0x41F);
                        state.videoRamAddr =
                            (updateVramAddr and 0xFBE0U) or (state.videoRamAddr and updateVramAddr and 0x41FU)
                    } else {
                        state.videoRamAddr = updateVramAddr
                    }
                } else {
                    state.videoRamAddr = updateVramAddr
                }

                // The glitches updates corrupt both V and T, so set the new value of V back into T
                state.tmpVideoRamAddr = state.videoRamAddr

                if (scanline >= 240 || !isRenderingEnabled) {
                    // Only set the VRAM address on the bus if the PPU is not rendering
                    // More info here: https://forums.nesdev.com/viewtopic.php?p=132145#p132145
                    // Trigger bus address change when setting the vram address - needed by MMC3 IRQ counter
                    // "4) Should be clocked when A12 changes to 1 via $2006 write"
                    setBusAddress(state.videoRamAddr and 0x3FFFU)
                }
            } else {
                isNeedStateUpdate = true
            }
        }

        if (ignoreVramRead > 0U) {
            ignoreVramRead--

            if (ignoreVramRead > 0U) {
                isNeedStateUpdate = true
            }
        }
    }

    private inline fun setOamCorruptionFlags() {
        if (!settings.checkFlag(EmulationFlag.ENABLE_PPU_OAM_ROW_CORRUPTION)) {
            return
        }

        // Note: Still pending more research, but this currently matches a portion of the issues that have been observed
        // When rendering is disabled in some sections of the screen, either:
        // A- During Secondary OAM clear (first ~64 cycles)
        // B- During OAM tile fetching (cycle ~256 to cycle ~320)
        // then OAM memory gets corrupted the next time the PPU starts rendering again (usually at the start of the next frame)
        // This usually causes the first "row" of OAM (the first 8 bytes) to get copied over another, causing some sprites to be missing
        // and causing an extra set of the first 2 sprites to appear on the screen (not possible to see them except via any overflow they may cause)

        if (cycle in 0..63) {
            // Every 2 dots causes the corruption to shift down 1 OAM row (8 bytes)
            corruptOamRow[cycle shr 1] = true
        } else if (cycle in 256..319) {
            // This section is in 8-dot segments.
            // The first 3 dot increment the corrupted row by 1, and then the last 5 dots corrupt the next row for 5 dots.
            val base = (cycle - 256) shr 3
            val offset = min(3, (cycle - 256) and 0x07)
            corruptOamRow[base * 4 + offset] = true
        }
    }

    private inline fun processSpriteEvaluation() {
        if (isRenderingEnabled || (region == Region.PAL && scanline >= palSpriteEvalScanline)) {
            if (cycle < 65) {
                // Clear secondary OAM at between cycle 1 and 64
                oamCopybuffer = 0xFFU
                secondarySpriteRAM[(cycle - 1) shr 1] = 0xFFU
            } else {
                if (cycle == 65) {
                    sprite0Added = false
                    spriteInRange = false
                    secondaryOAMAddr = 0U

                    overflowBugCounter = 0U

                    oamCopyDone = false
                    spriteAddrH = ((state.spriteRamAddr shr 2) and 0x3FU).toUByte()
                    spriteAddrL = (state.spriteRamAddr and 0x03U).toUByte()

                    firstVisibleSpriteAddr = (spriteAddrH.toUInt() * 4U).toUByte()
                    lastVisibleSpriteAddr = firstVisibleSpriteAddr
                } else if (cycle == 256) {
                    sprite0Visible = sprite0Added
                    spriteCount = secondaryOAMAddr shr 2
                }

                if ((cycle and 0x01) == 0x01) {
                    // Read a byte from the primary OAM on odd cycles
                    oamCopybuffer = readSpriteRam(state.spriteRamAddr.toUByte())
                } else {
                    if (oamCopyDone) {
                        spriteAddrH = spriteAddrH.plusOne() and 0x3FU

                        if (secondaryOAMAddr >= 0x20U) {
                            // As seen above, a side effect of the OAM write disable signal is to turn writes to the secondary OAM into reads from it.
                            oamCopybuffer = secondarySpriteRAM[(secondaryOAMAddr and 0x1FU).toInt()]
                        }
                    } else {
                        if (!spriteInRange &&
                            scanline >= oamCopybuffer.toInt() &&
                            scanline < oamCopybuffer.toInt() + if (flags.largeSprites) 16 else 8
                        ) {
                            spriteInRange = true
                        }

                        if (secondaryOAMAddr < 0x20U) {
                            // Copy 1 byte to secondary OAM
                            secondarySpriteRAM[secondaryOAMAddr.toInt()] = oamCopybuffer

                            if (spriteInRange) {
                                spriteAddrL++
                                secondaryOAMAddr++

                                if (spriteAddrH.isZero) {
                                    sprite0Added = true
                                }

                                // Note: Using "(_secondaryOAMAddr & 0x03) == 0" instead of "_spriteAddrL == 0" is required
                                // to replicate a hardware bug noticed in oam_flicker_test_reenable when disabling & re-enabling
                                // rendering on a single scanline
                                if ((secondaryOAMAddr and 0x03U) == 0U) {
                                    // Done copying all 4 bytes
                                    spriteInRange = false
                                    spriteAddrL = 0U
                                    lastVisibleSpriteAddr = (spriteAddrH.toUInt() * 4U).toUByte()
                                    spriteAddrH = spriteAddrH.plusOne() and 0x3FU

                                    if (spriteAddrH.isZero) {
                                        oamCopyDone = true
                                    }
                                }
                            } else {
                                // Nothing to copy, skip to next sprite
                                spriteAddrH = spriteAddrH.plusOne() and 0x3FU

                                if (spriteAddrH.isZero) {
                                    oamCopyDone = true
                                }
                            }
                        } else {
                            // As seen above, a side effect of the OAM write disable signal is to turn writes to the secondary OAM into reads from it.
                            oamCopybuffer = secondarySpriteRAM[(secondaryOAMAddr and 0x1FU).toInt()]

                            // 8 sprites have been found, check next sprite for overflow + emulate PPU bug
                            if (spriteInRange) {
                                // Sprite is visible, consider this to be an overflow
                                statusFlags.spriteOverflow = true
                                spriteAddrL++

                                if (spriteAddrL.toUInt() == 4U) {
                                    spriteAddrH = spriteAddrH.plusOne() and 0x3FU
                                    spriteAddrL = 0U
                                }

                                if (overflowBugCounter.isZero) {
                                    overflowBugCounter = 3U
                                } else {
                                    overflowBugCounter--

                                    if (overflowBugCounter.isZero) {
                                        // After it finishes "fetching" this sprite(and setting the overflow flag), it realigns back at the beginning of this line and then continues here on the next sprite
                                        oamCopyDone = true
                                        spriteAddrL = 0U
                                    }
                                }
                            } else {
                                // Sprite isn't on this scanline, trigger sprite evaluation bug - increment both H & L at the same time
                                spriteAddrH = spriteAddrH.plusOne() and 0x3FU
                                spriteAddrL = spriteAddrL.plusOne() and 0x03U

                                if (spriteAddrH.isZero) {
                                    oamCopyDone = true
                                }
                            }
                        }
                    }

                    state.spriteRamAddr = (spriteAddrL.toUInt() and 0x03U) or (spriteAddrH.toUInt() shl 2)
                }
            }
        }
    }

    private inline fun processOamCorruption() {
        if (!settings.checkFlag(EmulationFlag.ENABLE_PPU_OAM_ROW_CORRUPTION)) {
            return
        }

        // Copy first OAM row over another row, as needed by corruption flags (can be over itself, which causes no actual harm)
        for (i in 0..31) {
            if (corruptOamRow[i]) {
                if (i > 0) {
                    // memcpy(_spriteRAM + i * 8, _spriteRAM, 8)
                    spriteRAM.copyInto(spriteRAM, i * 8, 0, 8)
                }

                corruptOamRow[i] = false
            }
        }
    }

    fun getPixel(x: Int, y: Int): UShort {
        return currentOutputBuffer[(y shl 8) or x]
    }

    fun getPixelBrightness(x: Int, y: Int): UInt {
        val pixel = getPixel(x, y)
        val color = console.settings.palette[(pixel and 0x3FU).toInt()]
        return color.loByte + color.hiByte + color.higherByte
    }

    override fun getMemoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.READ, 0x2000U, 0x3FFFU)
        ranges.addHandler(MemoryOperation.WRITE, 0x2000U, 0x3FFFU)
        ranges.addHandler(MemoryOperation.WRITE, 0x4014U)
    }

    private inline fun getRegisterId(addr: UShort): PpuRegister {
        return if (addr.toUInt() == 0x4014U) {
            PpuRegister.SPRITE_DMA
        } else {
            PpuRegister.values()[addr.toInt() and 0x07]
        }
    }

    private inline fun processStatusRegOpenBus(result: UByte): Pair<UByte?, UByte?> {
        return when (settings.ppuModel) {
            PpuModel.PPU_2C05A -> Pair(0x00U, result or 0x1BU)
            PpuModel.PPU_2C05B -> Pair(0x00U, result or 0x3DU)
            PpuModel.PPU_2C05C -> Pair(0x00U, result or 0x1CU)
            PpuModel.PPU_2C05D -> Pair(0x00U, result or 0x1BU)
            PpuModel.PPU_2C05E -> Pair(0x00U, null)
            else -> Pair(null, null)
        }
    }

    private inline fun updateStatusFlag() {
        state.status = ((if (statusFlags.spriteOverflow) 0x20U else 0U) or
                (if (statusFlags.sprite0Hit) 0x40U else 0U) or
                (if (statusFlags.verticalBlank) 0x80U else 0U)).toUByte()

        statusFlags.verticalBlank = false
        console.cpu.nmi = false

        if (scanline == nmiScanline && cycle == 0) {
            // Reading one PPU clock before reads it as clear and never sets the flag or generates NMI for that frame
            preventVBlankFlag = true
        }
    }

    private inline fun updateVideoRamAddr() {
        if (scanline >= 240 || !isRenderingEnabled) {
            state.videoRamAddr = ((state.videoRamAddr + if (flags.verticalWrite) 32U else 1U) and 0x7FFFU).toUShort()

            // Trigger memory read when setting the vram address - needed by MMC3 IRQ counter
            // Should be clocked when A12 changes to 1 via $2007 read/write
            setBusAddress(state.videoRamAddr and 0x3FFFU)
        } else {
            // During rendering (on the pre-render line and the visible lines 0-239, provided either background or sprite rendering is enabled),
            // it will update v in an odd way, triggering a coarse X increment and a Y increment simultaneously
            incHorizontalScrolling()
            incVerticalScrolling()
        }
    }

    private inline fun incHorizontalScrolling() {
        // Taken from http://wiki.nesdev.com/w/index.php/The_skinny_on_NES_scrolling#Wrapping_around
        // Increase coarse X scrolling value.
        val addr = state.videoRamAddr

        state.videoRamAddr = if ((addr and 0x001FU).toUInt() == 31U) {
            // When the value is 31, wrap around to 0 and switch nametable
            // addr = (addr & ~0x001F) ^ 0x0400;
            (addr and 0xFFE0U) xor 0x0400U
        } else {
            addr.plusOne()
        }
    }

    private inline fun incVerticalScrolling() {
        val addr = state.videoRamAddr

        state.videoRamAddr = if ((addr.toUInt() and 0x7000U) != 0x7000U) {
            // if fine Y < 7
            (addr + 0x1000U).toUShort() // increment fine Y
        } else {
            // fine Y = 0
            var a = addr and 0x8FFFU // addr &= ~0x7000;
            var y = (a and 0x03E0U).toUInt() shr 5 // let y = coarse Y

            when (y) {
                29U -> {
                    y = 0U // coarse Y = 0
                    a = a xor 0x0800U // switch vertical nametable
                }
                31U -> {
                    y = 0U // coarse Y = 0, nametable not switched
                }
                else -> {
                    y++ // increment coarse Y
                }
            }

            // addr = (addr & ~0x03E0) | (y << 5);
            (a and 0xFC1FU) or (y shl 5).toUShort() // put coarse Y back into v
        }
    }

    inline val nametableAddress: UShort
        // Taken from http://wiki.nesdev.com/w/index.php/The_skinny_on_NES_scrolling#Tile_and_attribute_fetching
        get() = 0x2000U.toUShort() or (state.videoRamAddr and 0x0FFFU)

    inline val attributeAddress: UShort
        // Taken from http://wiki.nesdev.com/w/index.php/The_skinny_on_NES_scrolling#Tile_and_attribute_fetching
        get() = 0x23C0.toUShort() or (state.videoRamAddr and 0x0C00U) or ((state.videoRamAddr shr 4) and 0x38U) or ((state.videoRamAddr shr 2) and 0x07U)

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        var openBusMask: UByte = 0xFFU
        var result: UByte = 0U

        when (getRegisterId(addr)) {
            PpuRegister.STATUS -> {
                state.writeToggle = false
                updateStatusFlag()
                result = state.status
                openBusMask = 0x1FU

                val (a, b) = processStatusRegOpenBus(result)

                if (a != null) {
                    openBusMask = a
                }
                if (b != null) {
                    result = b
                }
            }
            PpuRegister.SPRITE_DATA -> {
                if (!settings.checkFlag(EmulationFlag.DISABLE_PPU_2004_READS)) {
                    if (scanline <= 239 && isRenderingEnabled) {
                        // While the screen is begin drawn
                        if (cycle in 257..320) {
                            // If we're doing sprite rendering, set OAM copy buffer to its proper value.  This is done here for performance.
                            // It's faster to only do this here when it's needed, rather than splitting LoadSpriteTileInfo() into an 8-step process
                            val step = min((cycle - 257) % 8, 3)
                            secondaryOAMAddr = (((cycle - 257) / 8) * 4 + step).toUInt()
                            oamCopybuffer = secondarySpriteRAM[secondaryOAMAddr.toInt()]
                        }

                        // Return the value that PPU is currently using for sprite evaluation/rendering
                        result = oamCopybuffer
                    } else {
                        result = readSpriteRam(state.spriteRamAddr.toUByte())
                    }

                    openBusMask = 0x00U
                }
            }
            PpuRegister.VIDEO_MEMORY_DATA -> {
                if (ignoreVramRead != 0U) {
                    // 2 reads to $2007 in quick succession (2 consecutive CPU cycles) causes the 2nd read to be ignored (normally depends on PPU/CPU timing, but this is the simplest solution)
                    // Return open bus in this case? (which will match the last value read)
                    openBusMask = 0xFFU
                } else {
                    result = memoryReadBuffer
                    memoryReadBuffer = readVRam(ppuBusAddress and 0x3FFFU)

                    if (ppuBusAddress and 0x3FFFU >= 0x3F00U && !settings.checkFlag(EmulationFlag.DISABLE_PALETTE_READ)) {
                        result = readPaletteRam(ppuBusAddress) or (openBus and 0xC0U)
                        openBusMask = 0xC0U
                    } else {
                        openBusMask = 0x00U
                    }

                    updateVideoRamAddr()

                    ignoreVramRead = 6U
                    isNeedStateUpdate = true
                }
            }
            else -> {
            }
        }

        return applyOpenBus(openBusMask, result)
    }

    override fun peek(addr: UShort): UByte {
        var openBusMask: UByte = 0xFFU
        var result: UByte = 0U

        when (getRegisterId(addr)) {
            PpuRegister.STATUS -> {
                result = ((if (statusFlags.spriteOverflow) 0x20U else 0x00U) or
                        (if (statusFlags.sprite0Hit) 0x40U else 0x00U) or
                        (if (statusFlags.verticalBlank) 0x80U else 0x00U)).toUByte()

                if (scanline == nmiScanline && cycle < 3) {
                    // Clear vertical blank flag
                    result = result and 0x7FU
                }

                openBusMask = 0x1FU

                val (a, b) = processStatusRegOpenBus(result)

                if (a != null) {
                    openBusMask = a
                }
                if (b != null) {
                    result = b
                }
            }
            PpuRegister.SPRITE_DATA -> {
                if (!settings.checkFlag(EmulationFlag.DISABLE_PPU_2004_READS)) {
                    result = if (scanline <= 239 && isRenderingEnabled) {
                        // While the screen is begin drawn
                        if (cycle in 257..320) {
                            // If we're doing sprite rendering, set OAM copy buffer to its proper value.  This is done here for performance.
                            // It's faster to only do this here when it's needed, rather than splitting LoadSpriteTileInfo() into an 8-step process
                            val step = min((cycle - 257) % 8, 3)
                            val a = ((cycle - 257) / 8) * 4 + step
                            secondarySpriteRAM[a]
                        } else {
                            oamCopybuffer
                        }
                    } else {
                        spriteRAM[state.spriteRamAddr.toInt()]
                    }

                    openBusMask = 0x00U
                }
            }
            PpuRegister.VIDEO_MEMORY_DATA -> {
                result = memoryReadBuffer

                if (state.videoRamAddr and 0x3FFFU >= 0x3F00U && !settings.checkFlag(EmulationFlag.DISABLE_PALETTE_READ)) {
                    result = readPaletteRam(state.videoRamAddr) or (openBus and 0xC0U)
                    openBusMask = 0xC0U
                } else {
                    openBusMask = 0x00U
                }
            }
            else -> {
            }
        }

        return result or (openBus and openBusMask)
    }

    private inline fun applyOpenBus(mask: UByte, value: UByte): UByte {
        setOpenBus(mask.inv(), value)
        return value or (openBus and mask)
    }

    private inline fun readPaletteRam(addr: UShort): UByte {
        var a = addr and 0x1FU

        if (a.toUInt() == 0x10U || a.toUInt() == 0x14U || a.toUInt() == 0x18U || a.toUInt() == 0x1CU) {
            a = a and 0xEFU // addr &= ~0x0010
        }

        return paletteRAM[a.toInt()]
    }

    protected inline fun readVRam(addr: UShort): UByte {
        setBusAddress(addr)
        return console.mapper!!.readVRAM(addr)
    }

    protected inline fun writeVRam(addr: UShort, value: UByte) {
        setBusAddress(addr)
        console.mapper!!.writeVRAM(addr, value)
    }

    protected inline fun readSpriteRam(addr: UByte): UByte {
        return if (!enableOamDecay) {
            spriteRAM[addr.toInt()]
        } else {
            val elapsedCycles = console.cpu.cycleCount - oamDecayCycles[addr.toInt() shr 3]

            if (elapsedCycles <= OAM_DECAY_CYCLE_COUNT) {
                oamDecayCycles[addr.toInt() shr 3] = console.cpu.cycleCount
                spriteRAM[addr.toInt()]
            } else {
                // If this 8-byte row hasn't been read/written to in over 3000 cpu cycles (~1.7ms), return 0x10 to simulate decay
                0x10U
            }
        }
    }

    protected inline fun writeSpriteRam(addr: UByte, value: UByte) {
        spriteRAM[addr.toInt()] = value

        if (enableOamDecay) {
            oamDecayCycles[addr.toInt() shr 3] = console.cpu.cycleCount
        }
    }

    private inline fun setOpenBus(mask: UByte, value: UByte) {
        // Decay expired bits, set new bits and update stamps on each individual bit
        if (mask.isFilled) {
            // Shortcut when mask is 0xFF - all bits are set to the value and stamps updated
            openBus = value
            openBusDecayStamp.fill(frameCount)
        } else {
            var m = mask
            var v = value

            var ob = (openBus.toUInt() shl 8).toUShort()

            for (i in 0..7) {
                ob = ob shr 1

                if (m.bit0) {
                    ob = if (v.bit0) {
                        ob or 0x80U
                    } else {
                        ob and 0xFF7FU
                    }

                    openBusDecayStamp[i] = frameCount
                } else if (frameCount - openBusDecayStamp[i] > 30) {
                    ob = ob and 0xFF7FU
                }

                v = v shr 1
                m = m shr 1
            }

            openBus = ob.toUByte()
        }
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        if (addr.toUInt() != 0x4014U) {
            setOpenBus(0xFFU, value)
        }

        val ppuModel = settings.ppuModel

        when (getRegisterId(addr)) {
            PpuRegister.CONTROL -> {
                if (ppuModel.is2C05) {
                    setMaskRegister(value)
                } else {
                    setControlRegister(value)
                }
            }
            PpuRegister.MASK -> {
                if (ppuModel.is2C05) {
                    setControlRegister(value)
                } else {
                    setMaskRegister(value)
                }
            }
            PpuRegister.SPRITE_ADDR -> {
                state.spriteRamAddr = value.toUInt()
            }
            PpuRegister.SPRITE_DATA -> {
                if ((scanline >= 240 && (region != Region.PAL || scanline < palSpriteEvalScanline)) || !isRenderingEnabled) {
                    if ((state.spriteRamAddr and 0x03U) == 0x02U) {
                        // The three unimplemented bits of each sprite's byte 2 do not exist in the PPU
                        // and always read back as 0 on PPU revisions that allow reading PPU OAM through OAMDATA ($2004)
                        writeSpriteRam(state.spriteRamAddr.toUByte(), value and 0xE3U)
                    } else {
                        writeSpriteRam(state.spriteRamAddr.toUByte(), value)
                    }

                    state.spriteRamAddr = (state.spriteRamAddr + 1U) and 0xFFU
                } else {
                    // Writes to OAMDATA during rendering (on the pre-render line and the visible lines 0-239,
                    // provided either sprite or background rendering is enabled) do not modify values in OAM,
                    // but do perform a glitchy increment of OAMADDR, bumping only the high 6 bits
                    state.spriteRamAddr = (state.spriteRamAddr + 4U) and 0xFFU
                }
            }
            PpuRegister.SCROLL_OFFSET -> {
                if (state.writeToggle) {
                    // and ~0x73E0
                    state.tmpVideoRamAddr =
                        (state.tmpVideoRamAddr and 0x8C1FU) or ((value and 0xF8U).toUInt() shl 2).toUShort() or ((value and 0x07U).toUInt() shl 12).toUShort()
                } else {
                    state.xScroll = value and 0x07U
                    // and ~0x001F
                    val newAddr = (state.tmpVideoRamAddr and 0xFFE0U) or (value.toUShort() shr 3)
                    processTmpAddrScrollGlitch(newAddr, console.memoryManager.getOpenBus().toUShort() shr 3, 0x001FU)
                }

                state.writeToggle = !state.writeToggle
            }
            PpuRegister.VIDEO_MEMORY_ADDR -> {
                if (state.writeToggle) {
                    // and ~0x00FF
                    state.tmpVideoRamAddr = (state.tmpVideoRamAddr and 0xFF00U) or value.toUShort()

                    // Video RAM update is apparently delayed by 3 PPU cycles (based on Visual NES findings)
                    isNeedStateUpdate = true
                    updateVramAddrDelay = 3U
                    updateVramAddr = state.tmpVideoRamAddr
                } else {
                    // and ~0xFF00
                    val newAddr = (state.tmpVideoRamAddr and 0x00FFU) or ((value and 0x3FU).toUInt() shl 8).toUShort()
                    processTmpAddrScrollGlitch(
                        newAddr,
                        (console.memoryManager.getOpenBus().toUInt() shl 8).toUShort(),
                        0x0C00U
                    )
                }

                state.writeToggle = !state.writeToggle
            }
            PpuRegister.VIDEO_MEMORY_DATA -> {
                // The palettes start at PPU address $3F00 and $3F10.
                if ((ppuBusAddress and 0x3FFFU) >= 0x3F00U) {
                    writePaletteRam(ppuBusAddress, value)
                } else if (scanline >= 240 || !isRenderingEnabled) {
                    console.mapper!!.writeVRAM(ppuBusAddress and 0x3FFFU, value)
                } else {
                    // During rendering, the value written is ignored, and instead the address' LSB is used (not confirmed, based on Visual NES)
                    console.mapper!!.writeVRAM(ppuBusAddress and 0x3FFFU, ppuBusAddress.toUByte())
                }

                updateVideoRamAddr()
            }
            PpuRegister.SPRITE_DMA -> {
                console.cpu.runDmaTransfer(value)
            }
            else -> {

            }
        }
    }

    private inline fun setMaskRegister(value: UByte) {
        state.mask = value

        flags.grayscale = state.mask.bit0
        flags.backgroundMask = state.mask.bit1
        flags.spriteMask = state.mask.bit2
        flags.backgroundEnabled = state.mask.bit3
        flags.spritesEnabled = state.mask.bit4
        flags.intensifyBlue = state.mask.bit7

        if (isRenderingEnabled != (flags.backgroundEnabled or flags.spritesEnabled)) {
            isNeedStateUpdate = true
        }

        updateMinimumDrawCycles()
        updateGrayscaleAndIntensifyBits()

        //"Bit 0 controls a greyscale mode, which causes the palette to use only the colors from the grey column: $00, $10, $20, $30. This is implemented as a bitwise AND with $30 on any value read from PPU $3F00-$3FFF"
        paletteRamMask = if (flags.grayscale) 0x30U else 0x3FU

        if (region == Region.NTSC) {
            flags.intensifyRed = state.mask.bit5
            flags.intensifyGreen = state.mask.bit6
            intensifyColorBits = ((value and 0xE0U).toUInt() shl 1).toUShort()
        } else if (region == Region.PAL || region == Region.DENDY) {
            // Note that on the Dendy and PAL NES, the green and red bits swap meaning.
            flags.intensifyRed = state.mask.bit6
            flags.intensifyGreen = state.mask.bit5
            intensifyColorBits =
                ((if (flags.intensifyRed) 0x40U else 0x00U) or (if (flags.intensifyGreen) 0x80U else 0x00U) or (if (flags.intensifyBlue) 0x100U else 0x00U)).toUShort()
        }
    }

    private inline fun setControlRegister(value: UByte) {
        state.control = value

        val nameTable = state.control and 0x03U
        val addr = (state.tmpVideoRamAddr and 0xF3FFU) or (nameTable.toUInt() shl 10).toUShort() // and ~0x0C00
        processTmpAddrScrollGlitch(addr, (console.memoryManager.getOpenBus().toInt() shl 10).toUShort(), 0x0400U)

        flags.verticalWrite = state.control.bit2
        flags.spritePatternAddr = if (state.control.bit3) 0x1000U else 0x0000U
        flags.backgroundPatternAddr = if (state.control.bit4) 0x1000U else 0x0000U
        flags.largeSprites = state.control.bit5
        flags.vBlank = state.control.bit7

        // By toggling NMI_output ($2000 bit 7) during vertical blank without reading $2002, a program can cause /NMI to be pulled low multiple times, causing multiple NMIs to be generated.
        if (!flags.vBlank) {
            console.cpu.nmi = false
        } else if (flags.vBlank && statusFlags.verticalBlank) {
            console.cpu.nmi = true
        }
    }

    inline fun setBusAddress(addr: UShort) {
        ppuBusAddress = addr
        console.mapper!!.notifyVRAMAddressChange(addr)
    }

    private inline fun processTmpAddrScrollGlitch(addr: UShort, value: UShort, mask: UShort) {
        state.tmpVideoRamAddr = addr

        if (cycle == 257 && settings.checkFlag(EmulationFlag.ENABLE_PPU_2000_SCROLL_GLITCH) &&
            scanline < 240 &&
            isRenderingEnabled
        ) {
            // Use open bus to set some parts of V (glitch that occurs when writing to $2000/$2005/$2006 on cycle 257)
            state.videoRamAddr = (state.videoRamAddr and mask.inv()) or (value and mask)
        }
    }

    private inline fun writePaletteRam(addr: UShort, value: UByte) {
        val a = (addr and 0x1FU).toUInt()
        val b = value and 0x3FU

        if (a == 0x00U || a == 0x10U) {
            paletteRAM[0x00] = b
            paletteRAM[0x10] = b
        } else if (a == 0x04U || a == 0x14U) {
            paletteRAM[0x04] = b
            paletteRAM[0x14] = b
        } else if (a == 0x08U || a == 0x18U) {
            paletteRAM[0x08] = b
            paletteRAM[0x18] = b
        } else if (a == 0x0CU || a == 0x1CU) {
            paletteRAM[0x0C] = b
            paletteRAM[0x1C] = b
        } else {
            paletteRAM[a.toInt()] = b
        }
    }

    override fun saveState(s: Snapshot) {
        val disablePpu2004Reads = console.settings.checkFlag(EmulationFlag.DISABLE_PPU_2004_READS)
        val disablePaletteRead = console.settings.checkFlag(EmulationFlag.DISABLE_PALETTE_READ)
        val disableOamAddrBug = console.settings.checkFlag(EmulationFlag.DISABLE_OAM_ADDR_BUG)

        s.write("state", state)
        s.write("flags", flags)
        s.write("paletteRamMask", paletteRamMask)
        s.write("intensifyColorBits", intensifyColorBits)
        s.write("statusFlags", statusFlags)
        s.write("scanline", scanline)
        s.write("cycle", cycle)
        s.write("frameCount", frameCount)
        s.write("memoryReadBuffer", memoryReadBuffer)
        s.write("previousTile", previousTile)
        s.write("currentTile", currentTile)
        s.write("nextTile", nextTile)
        s.write("spriteIndex", spriteIndex)
        s.write("spriteCount", spriteCount)
        s.write("secondaryOAMAddr", secondaryOAMAddr)
        s.write("sprite0Visible", sprite0Visible)
        s.write("oamCopybuffer", oamCopybuffer)
        s.write("spriteInRange", spriteInRange)
        s.write("sprite0Added", sprite0Added)
        s.write("spriteAddrH", spriteAddrH)
        s.write("spriteAddrL", spriteAddrL)
        s.write("oamCopyDone", oamCopyDone)
        s.write("region", region)
        s.write("prevIsRenderingEnabled", prevIsRenderingEnabled)
        s.write("isRenderingEnabled", isRenderingEnabled)
        s.write("openBus", openBus)
        s.write("ignoreVramRead", ignoreVramRead)
        s.write("paletteRAM", paletteRAM)
        s.write("spriteRAM", spriteRAM)
        s.write("secondarySpriteRAM", secondarySpriteRAM)
        s.write("openBusDecayStamp", openBusDecayStamp)
        s.write("disablePpu2004Reads", disablePpu2004Reads)
        s.write("disablePaletteRead", disablePaletteRead)
        s.write("disableOamAddrBug", disableOamAddrBug)
        s.write("overflowBugCounter", overflowBugCounter)
        s.write("updateVramAddr", updateVramAddr)
        s.write("updateVramAddrDelay", updateVramAddrDelay)
        s.write("isNeedStateUpdate", isNeedStateUpdate)
        s.write("ppuBusAddress", ppuBusAddress)
        s.write("preventVBlankFlag", preventVBlankFlag)
        s.write("masterClock", masterClock)

        spriteTiles.forEachIndexed { index, info -> s.write("spriteTile$index", info) }
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        s.readSnapshot("state")?.let { state.restoreState(it) }
        s.readSnapshot("flags")?.let { flags.restoreState(it) }
        paletteRamMask = s.readUByte("paletteRamMask") ?: 0U
        intensifyColorBits = s.readUShort("intensifyColorBits") ?: 0U
        s.readSnapshot("statusFlags")?.let { statusFlags.restoreState(it) }
        scanline = s.readInt("scanline") ?: 0
        cycle = s.readInt("cycle") ?: 0
        frameCount = s.readInt("frameCount") ?: 0
        memoryReadBuffer = s.readUByte("memoryReadBuffer") ?: 0U
        s.readSnapshot("previousTile")?.let { previousTile.restoreState(it) }
        s.readSnapshot("currentTile")?.let { currentTile.restoreState(it) }
        s.readSnapshot("nextTile")?.let { nextTile.restoreState(it) }
        spriteIndex = s.readUInt("spriteIndex") ?: 0U
        spriteCount = s.readUInt("spriteCount") ?: 0U
        secondaryOAMAddr = s.readUInt("secondaryOAMAddr") ?: 0U
        sprite0Visible = s.readBoolean("sprite0Visible") ?: false
        oamCopybuffer = s.readUByte("oamCopybuffer") ?: 0U
        spriteInRange = s.readBoolean("spriteInRange") ?: false
        sprite0Added = s.readBoolean("sprite0Added") ?: false
        spriteAddrH = s.readUByte("spriteAddrH") ?: 0U
        spriteAddrL = s.readUByte("spriteAddrL") ?: 0U
        oamCopyDone = s.readBoolean("oamCopyDone") ?: false
        region = s.readEnum("region") ?: Region.AUTO
        prevIsRenderingEnabled = s.readBoolean("prevIsRenderingEnabled") ?: false
        isRenderingEnabled = s.readBoolean("isRenderingEnabled") ?: false
        openBus = s.readUByte("openBus") ?: 0U
        ignoreVramRead = s.readUInt("ignoreVramRead") ?: 0U
        s.readUByteArray("paletteRAM")?.copyInto(paletteRAM)
        s.readUByteArray("spriteRAM")?.copyInto(spriteRAM)
        s.readUByteArray("secondarySpriteRAM")?.copyInto(secondarySpriteRAM)
        s.readIntArray("openBusDecayStamp")?.copyInto(openBusDecayStamp)
        val disablePpu2004Reads = s.readBoolean("disablePpu2004Reads") ?: false
        val disablePaletteRead = s.readBoolean("disablePaletteRead") ?: false
        val disableOamAddrBug = s.readBoolean("disableOamAddrBug") ?: false
        overflowBugCounter = s.readUByte("overflowBugCounter") ?: 0U
        updateVramAddr = s.readUShort("updateVramAddr") ?: 0U
        updateVramAddrDelay = s.readUByte("updateVramAddrDelay") ?: 0U
        isNeedStateUpdate = s.readBoolean("isNeedStateUpdate") ?: false
        ppuBusAddress = s.readUShort("ppuBusAddress") ?: 0U
        preventVBlankFlag = s.readBoolean("preventVBlankFlag") ?: false
        masterClock = s.readLong("masterClock") ?: 0L

        spriteTiles.indices.forEach { i -> s.readSnapshot("spriteTile$i")?.let { spriteTiles[i].restoreState(it) } }

        if (disablePpu2004Reads) console.settings.setFlag(EmulationFlag.DISABLE_PPU_2004_READS)
        else console.settings.clearFlag(EmulationFlag.DISABLE_PPU_2004_READS)

        if (disablePaletteRead) console.settings.setFlag(EmulationFlag.DISABLE_PALETTE_READ)
        else console.settings.clearFlag(EmulationFlag.DISABLE_PALETTE_READ)

        if (disableOamAddrBug) console.settings.setFlag(EmulationFlag.DISABLE_OAM_ADDR_BUG)
        else console.settings.clearFlag(EmulationFlag.DISABLE_OAM_ADDR_BUG)

        updateMinimumDrawCycles()

        for (i in 0 until 0x20) {
            // Set oam decay cycle to the current cycle to ensure it doesn't decay when loading a state
            oamDecayCycles[i] = console.cpu.cycleCount
        }

        corruptOamRow.fill(false)
        hasSprite.fill(false)

        lastUpdatedPixel = -1

        updateApuStatus()
    }

    fun getScreenBuffer(previous: Boolean): UShortArray {
        return if (previous) {
            if (currentOutputBuffer == outputBuffers[0]) outputBuffers[1] else outputBuffers[0]
        } else {
            currentOutputBuffer
        }
    }

    private inline fun processPpuCycle() {
        console.debugger.processPpuCycle()
    }

    companion object {
        const val SCREEN_WIDTH = 256
        const val SCREEN_HEIGHT = 240
        const val PIXEL_COUNT = SCREEN_WIDTH * SCREEN_HEIGHT
        const val OAM_DECAY_CYCLE_COUNT = 3000L
    }
}
