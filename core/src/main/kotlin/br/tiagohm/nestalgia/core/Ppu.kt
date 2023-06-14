package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.EmulationFlag.*
import br.tiagohm.nestalgia.core.PpuModel.*
import br.tiagohm.nestalgia.core.PpuRegister.*
import br.tiagohm.nestalgia.core.RamPowerOnState.*
import br.tiagohm.nestalgia.core.Region.*
import kotlin.math.min
import kotlin.random.Random

@Suppress("NOTHING_TO_INLINE")
open class Ppu(private val console: Console) : MemoryHandler, Resetable, Initializable, Snapshotable {

    private val settings = console.settings
    private var standardVBlankEnd = 0
    private var standardNmiScanline = 0
    private var vBlankEnd = 0
    private var nmiScanline = 0
    private var palSpriteEvalScanline = 0

    @JvmField protected var masterClock = 0L
    @JvmField protected var masterClockDivider = 4
    private var memoryReadBuffer = 0

    private val outputBuffers = arrayOf(IntArray(PIXEL_COUNT), IntArray(PIXEL_COUNT))

    private var currentOutputBuffer = outputBuffers[0]

    private val paletteRAM = IntArray(32)
    private val spriteRAM = IntArray(0x100)
    private val secondarySpriteRAM = IntArray(0x20)
    private val hasSprite = BooleanArray(257)

    private var spriteCount = 0
    private var secondaryOAMAddr = 0
    private var sprite0Visible = false

    private var firstVisibleSpriteAddr = 0
    private var lastVisibleSpriteAddr = 0
    private var spriteIndex = 0

    private var intensifyColorBits = 0
    private var paletteRamMask = 0
    private var lastUpdatedPixel = 0

    private var lastSprite: SpriteInfo? = null

    private val spriteTiles = Array(64) { SpriteInfo() }

    private val openBusDecayStamp = IntArray(8)
    private var ignoreVramRead = 0

    private var oamCopybuffer = 0
    private var spriteInRange = false
    private var sprite0Added = false
    private var spriteAddrH = 0
    private var spriteAddrL = 0
    private var oamCopyDone = false
    private var overflowBugCounter = 0

    private var ppuBusAddress = 0

    private var needStateUpdate = false
    private var renderingEnabled = false
    private var prevRenderingEnabled = false
    private var preventVBlankFlag = false

    private val oamDecayCycles = LongArray(0x40)
    private val corruptOamRow = BooleanArray(32)
    private var enableOamDecay = false

    private var updateVramAddr = 0
    private var updateVramAddrDelay = 0

    private val state = PpuState()

    private val flags = PpuControl()
    private val statusFlags = PpuStatus()

    private var minimumDrawBgCycle = 0
    private var minimumDrawSpriteCycle = 0
    private var minimumDrawSpriteStandardCycle = 0

    private var tile = TileInfo()
    private var currentTilePalette = 0
    private var previousTilePalette = 0

    private var needVideoRamIncrement = false

    var scanline = 0
        private set

    var cycle = 0
        protected set

    var frameCount = 0
        private set

    val frameCycle
        get() = (scanline + 1) * 341 + cycle

    private var openBus = 0

    override fun initialize() {
        console.initializeRam(spriteRAM)
        console.initializeRam(secondarySpriteRAM)

        reset(false)

        if (console.settings.ramPowerOnState == RANDOM) {
            console.initializeRam(paletteRAM)
            for (i in paletteRAM.indices) paletteRAM[i] = paletteRAM[i] and 0x3F
        } else {
            PALETTE_BOOT_RAM.copyInto(paletteRAM)
        }

        updateRegion(console.region)
    }

    override fun reset(softReset: Boolean) {
        masterClock = 0L
        preventVBlankFlag = false

        needStateUpdate = false
        prevRenderingEnabled = false
        renderingEnabled = false

        ignoreVramRead = 0
        openBus = 0

        openBusDecayStamp.fill(0)

        state.reset(softReset)
        flags.reset(softReset)
        statusFlags.reset(softReset)

        if (!softReset) {
            // The VBL flag (PPUSTATUS bit 7) is random at power, and unchanged by reset.
            statusFlags.verticalBlank = Random.nextBoolean()
        }

        tile = TileInfo()
        currentTilePalette = 0
        previousTilePalette = 0

        ppuBusAddress = 0
        intensifyColorBits = 0
        paletteRamMask = 0x3F
        lastUpdatedPixel = -1
        lastSprite = null
        oamCopybuffer = 0
        spriteInRange = false
        sprite0Added = false
        spriteAddrH = 0
        spriteAddrL = 0
        oamCopyDone = false

        hasSprite.fill(false)
        spriteTiles.indices.forEach { i -> spriteTiles[i] = SpriteInfo() }

        spriteCount = 0
        secondaryOAMAddr = 0
        sprite0Visible = false
        spriteIndex = 0
        openBus = 0

        ignoreVramRead = 0

        // First execution will be cycle 0, scanline 0
        scanline = -1
        cycle = 340

        frameCount = 1
        memoryReadBuffer = 0

        overflowBugCounter = 0

        updateVramAddrDelay = 0
        updateVramAddr = 0

        oamDecayCycles.fill(0)
        enableOamDecay = settings.flag(ENABLE_OAM_DECAY)

        updateMinimumDrawCycles()
    }

    private fun updateMinimumDrawCycles() {
        minimumDrawBgCycle =
            if (flags.backgroundEnabled) if (flags.backgroundMask || settings.flag(FORCE_BACKGROUND_FIRST_COLUMN)) 0 else 8 else 300
        minimumDrawSpriteCycle =
            if (flags.spritesEnabled) if (flags.spriteMask || settings.flag(FORCE_SPRITES_FIRST_COLUMN)) 0 else 8 else 300
        minimumDrawSpriteStandardCycle = if (flags.spritesEnabled) if (flags.spriteMask) 0 else 8 else 300
    }

    fun updateRegion(region: Region) {
        when (region) {
            AUTO -> Unit
            NTSC -> {
                nmiScanline = 241
                vBlankEnd = 260
                standardNmiScanline = 241
                standardVBlankEnd = 260
                masterClockDivider = 4
            }
            PAL -> {
                nmiScanline = 241
                vBlankEnd = 310
                standardNmiScanline = 241
                standardVBlankEnd = 310
                masterClockDivider = 5
            }
            DENDY -> {
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

    val overclockRate
        get() = (vBlankEnd.toDouble() + 2) / (2 + when (console.region) {
            PAL -> 310
            DENDY -> 310
            else -> 260
        })

    open fun run(runTo: Long) {
        while (masterClock + masterClockDivider <= runTo) {
            exec()
            masterClock += masterClockDivider
        }
    }

    protected fun processScanlineFirstCycle() {
        cycle = 0

        if (++scanline > vBlankEnd) {
            lastUpdatedPixel = -1
            scanline = -1

            // Force prerender scanline sprite fetches to load
            // the dummy $FF tiles (fixes shaking in Ninja Gaiden 3 stage 1
            // after beating boss).
            spriteCount = 0

            if (renderingEnabled) {
                processOamCorruption()
            }

            updateMinimumDrawCycles()
        }

        updateApuStatus()

        if (scanline == settings.inputPollScanline) {
            console.controlManager.updateControlDevices()
            console.controlManager.updateInputState()
        }

        if (scanline < 240) {
            if (scanline == -1) {
                statusFlags.spriteOverflow = false
                statusFlags.sprite0Hit = false

                // Switch to alternate output buffer (VideoDecoder may still be decoding the last frame buffer)
                currentOutputBuffer = if (currentOutputBuffer === outputBuffers[0]) outputBuffers[1] else outputBuffers[0]
            } else if (prevRenderingEnabled) {
                if (scanline > 0 || (!frameCount.bit0 || console.region != NTSC || settings.ppuModel != PPU_2C02)) {
                    // Set bus address to the tile address calculated from the unused NT fetches at the end of the previous scanline
                    // This doesn't happen on scanline 0 if the last dot of the previous frame was skipped
                    busAddress((tile.tileAddr shl 4) or (state.videoRamAddr shr 12) or flags.backgroundPatternAddr)
                }
            }
        } else if (scanline == 240) {
            // At the start of vblank, the bus address is set back to VideoRamAddr.
            // According to Visual NES, this occurs on scanline 240, cycle 1, but is done here on cycle for performance reasons
            busAddress(state.videoRamAddr and 0x3FFF)
            sendFrame()
            frameCount++
        }
    }

    private fun exec() {
        if (cycle >= 340) {
            processScanlineFirstCycle()
        } else {
            // Process cycles 1 to 340.
            cycle++

            if (scanline < 240) {
                processScanline()
            } else if (cycle == 1 && scanline == nmiScanline) {
                if (!preventVBlankFlag) {
                    statusFlags.verticalBlank = true
                    beginVBlank()
                }

                preventVBlankFlag = false
            } else if (console.region == PAL && scanline >= palSpriteEvalScanline) {
                // On a PAL machine, because of its extended vertical blank, the PPU begins refreshing OAM roughly 21 scanlines after NMI[2], to prevent it
                // from decaying during the longer hiatus of rendering. Additionally, it will continue to refresh during the visible portion of the screen
                // even if rendering is disabled. Because of this, OAM DMA must be done near the beginning of vertical blank on PAL, and everywhere else
                // it is liable to conflict with the refresh. Since the refresh can't be disabled like on the NTSC hardware, OAM decay does not occur at all on the PAL NES.
                if (cycle <= 256) {
                    processSpriteEvaluation()
                } else if (cycle in 257..319) {
                    state.spriteRamAddr = 0
                }
            }
        }

        if (needStateUpdate) {
            updateState()
        }

        processPpuCycle()
    }

    private fun updateApuStatus() {
        val apu = console.apu

        apu.enabled = true

        if (scanline > 240) {
            if (scanline > standardVBlankEnd) {
                // Disable APU for extra lines after NMI
                apu.enabled = false
            } else if (scanline in standardNmiScanline until nmiScanline) {
                // Disable APU for extra lines before NMI
                apu.enabled = false
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

        enableOamDecay = settings.flag(ENABLE_OAM_DECAY)
    }

    private fun updateGrayscaleAndIntensifyBits() {
        if (scanline < 0 || scanline > nmiScanline) {
            updateColorBitMasks()
            return
        }

        val pixelNumber = when {
            scanline >= 240 -> 61439
            cycle < 3 -> (scanline shl 8) - 1
            cycle <= 258 -> (scanline shl 8) + cycle - 3
            else -> (scanline shl 8) + 255
        }

        if (paletteRamMask == 0x3F && intensifyColorBits == 0) {
            // Nothing to do (most common case)
            updateColorBitMasks()
            lastUpdatedPixel = pixelNumber
            return
        }

        if (lastUpdatedPixel < pixelNumber) {
            var out = lastUpdatedPixel + 1

            while (lastUpdatedPixel < pixelNumber) {
                currentOutputBuffer[out] = (currentOutputBuffer[out] and paletteRamMask) or intensifyColorBits
                out++
                lastUpdatedPixel++
            }
        }

        updateColorBitMasks()
    }

    private fun updateColorBitMasks() {
        // Bit 0 controls a greyscale mode, which causes the palette to use only the colors from the grey column: $00, $10, $20, $30.
        // This is implemented as a bitwise AND with $30 on any value read from PPU $3F00-$3FFF
        paletteRamMask = if (flags.grayscale) 0x30 else 0x3F
        intensifyColorBits = (if (flags.intensifyRed) 0x40 else 0x00) or
            (if (flags.intensifyGreen) 0x80 else 0x00) or
            (if (flags.intensifyBlue) 0x100 else 0x00)
    }

    private fun processScanline() {
        // Only called for cycle 1+
        if (cycle <= 256) {
            loadTileInfo()

            if (prevRenderingEnabled && (cycle and 0x07) == 0) {
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

                if (state.spriteRamAddr >= 0x08 && renderingEnabled && !settings.flag(DISABLE_OAM_ADDR_BUG)) {
                    // This should only be done if rendering is enabled (otherwise oam_stress test fails immediately)
                    // If OAMADDR is not less than eight when rendering starts, the eight bytes starting at OAMADDR & 0xF8 are copied to the first eight bytes of OAM
                    writeSpriteRam(
                        cycle - 1,
                        readSpriteRam((state.spriteRamAddr and 0xF8) + cycle - 1)
                    )
                }
            }
        } else if (cycle in 257..320) {
            if (cycle == 257) {
                spriteIndex = 0
                hasSprite.fill(false)

                if (prevRenderingEnabled) {
                    // copy horizontal scrolling value from t
                    state.videoRamAddr = (state.videoRamAddr and 0x041F.inv()) or (state.tmpVideoRamAddr and 0x041F)
                }
            }

            if (renderingEnabled) {
                // OAMADDR is set to 0 during each of ticks 257-320 (the sprite tile loading interval) of the pre-render and visible scanlines. (When rendering)
                state.spriteRamAddr = 0

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
                    state.videoRamAddr = (state.videoRamAddr and 0x841F) or (state.tmpVideoRamAddr and 0x7BE0)
                }
            }
        } else if (cycle in 321..336) {
            loadTileInfo()

            if (cycle == 321) {
                if (renderingEnabled) {
                    loadExtraSprites()
                    oamCopybuffer = secondarySpriteRAM[0]
                }
            } else if (prevRenderingEnabled && (cycle == 328 || cycle == 336)) {
                state.lowBitShift = state.lowBitShift shl 8
                state.highBitShift = state.highBitShift shl 8
                incHorizontalScrolling()
            }
        } else if (cycle == 337 || cycle == 339) {
            if (renderingEnabled) {
                tile.tileAddr = readVRam(nametableAddress)

                if (scanline == -1 &&
                    cycle == 339 &&
                    (frameCount and 0x01) == 0x01 &&
                    console.region == NTSC &&
                    settings.ppuModel == PPU_2C02
                ) {
                    // This behavior is NTSC-specific - PAL frames are always the same number of cycles
                    // With rendering enabled, each odd PPU frame is one PPU clock shorter than normal" (skip from 339 to 0, going over 340)
                    cycle = 340
                }
            }
        }
    }

    private fun loadSpriteTileInfo() {
        val a = spriteIndex * 4

        loadSprite(
            secondarySpriteRAM[a],
            secondarySpriteRAM[a + 1],
            secondarySpriteRAM[a + 2],
            secondarySpriteRAM[a + 3],
            false
        )
    }

    private fun loadExtraSprites() {
        if (spriteCount == 8 && settings.flag(REMOVE_SPRITE_LIMIT)) {
            var loadExtraSprites = true

            if (settings.flag(ADAPTIVE_SPRITE_LIMIT)) {
                var lastPosition = -1
                var identicalSpriteCount = 0
                var maxIdenticalSpriteCount = 0

                repeat(64) {
                    val y = spriteRAM[it shl 2]

                    if (scanline >= y &&
                        scanline < y + (if (flags.largeSprites) 16 else 8)
                    ) {
                        val x = spriteRAM[(it shl 2) + 3]
                        val position = (y shl 8) or x

                        if (lastPosition != position) {
                            if (identicalSpriteCount > maxIdenticalSpriteCount) {
                                maxIdenticalSpriteCount = identicalSpriteCount
                            }

                            lastPosition = position
                            identicalSpriteCount = 1
                        } else {
                            identicalSpriteCount++
                        }
                    }
                }

                loadExtraSprites = identicalSpriteCount < 8 && maxIdenticalSpriteCount < 8
            }

            if (loadExtraSprites) {
                var i = (lastVisibleSpriteAddr + 4) and 0xFC

                while (i != firstVisibleSpriteAddr and 0xFC) {
                    val spriteY = spriteRAM[i]

                    if (scanline >= spriteY &&
                        scanline < spriteY + (if (flags.largeSprites) 16 else 8)
                    ) {
                        loadSprite(
                            spriteY,
                            spriteRAM[i + 1],
                            spriteRAM[i + 2],
                            spriteRAM[i + 3],
                            true,
                        )

                        spriteCount++
                    }

                    i = (i + 4) and 0xFC
                }
            }
        }
    }

    private inline fun drawPixel() {
        // This is called 3.7 million times per second - needs to be as fast as possible.
        if (renderingEnabled || (state.videoRamAddr and 0x3F00) != 0x3F00) {
            val color = getPixelColor()
            currentOutputBuffer[(scanline shl 8) + cycle - 1] = paletteRAM[if (color and 0x03 != 0) color else 0] and paletteRamMask
        } else {
            // If the current VRAM address points in the range $3F00-$3FFF during forced blanking,
            // the color indicated by this palette location will be shown on screen instead of the backdrop color.
            currentOutputBuffer[(scanline shl 8) + cycle - 1] = paletteRAM[state.videoRamAddr and 0x1F] and paletteRamMask
        }
    }

    fun currentBgColor(): Int {
        val color = if (renderingEnabled || (state.videoRamAddr and 0x3F00) != 0x3F00) {
            paletteRAM[0]
        } else {
            paletteRAM[state.videoRamAddr and 0x1F]
        }

        return (color and paletteRamMask) or intensifyColorBits
    }

    private fun getPixelColor(): Int {
        val offset = state.xScroll
        var backgroundColor = 0
        var spriteBgColor = 0

        if (cycle > minimumDrawBgCycle) {
            // BackgroundMask = false: Hide background in leftmost 8 pixels of screen
            spriteBgColor = (state.lowBitShift shl offset and 0x8000 shr 15) or (state.highBitShift shl offset and 0x8000 shr 14)

            if (settings.backgroundEnabled) {
                backgroundColor = spriteBgColor
            }
        }

        if (hasSprite[cycle] && cycle > minimumDrawSpriteCycle) {
            for (i in 0 until spriteCount) {
                val shift = cycle - spriteTiles[i].spriteX - 1

                if (shift in 0..7) {
                    lastSprite = spriteTiles[i]

                    val spriteColor = if (spriteTiles[i].horizontalMirror) {
                        (lastSprite!!.lowByte shr shift and 0x01) or (lastSprite!!.highByte shr shift and 0x01 shl 1)
                    } else {
                        ((lastSprite!!.lowByte shl shift and 0x80) shr 7) or (lastSprite!!.highByte shl shift and 0x80 shr 6)
                    }

                    if (spriteColor != 0) {
                        // First sprite without a 00 color, use it.
                        if (i == 0 &&
                            spriteBgColor != 0 &&
                            sprite0Visible &&
                            cycle != 256 &&
                            flags.backgroundEnabled &&
                            !statusFlags.sprite0Hit &&
                            cycle > minimumDrawSpriteStandardCycle
                        ) {
                            // The hit condition is basically sprite zero is in range AND the first sprite output unit is outputting a non-zero pixel AND the background drawing unit is outputting a non-zero pixel.
                            // Sprite zero hits do not register at x=255 (cycle 256)
                            // ...provided that background and sprite rendering are both enabled
                            // Should always miss when Y >= 239
                            statusFlags.sprite0Hit = true
                        }

                        if (settings.spritesEnabled &&
                            (backgroundColor == 0 || !spriteTiles[i].backgroundPriority)
                        ) {
                            // Check sprite priority
                            return lastSprite!!.paletteOffset + spriteColor
                        }

                        break
                    }
                }
            }
        }

        return (if (offset + ((cycle - 1) and 0x07) < 8) previousTilePalette else currentTilePalette) + backgroundColor
    }

    private fun shiftTileRegisters() {
        state.lowBitShift = state.lowBitShift shl 1
        state.highBitShift = state.highBitShift shl 1
    }

    private fun loadSprite(
        spriteY: Int,
        tileIndex: Int,
        attributes: Int,
        spriteX: Int,
        extraSprite: Boolean,
    ) {
        var mTileIndex = tileIndex
        val backgroundPriority = attributes.bit5
        val horizontalMirror = attributes.bit6
        val verticalMirror = attributes.bit7

        val lineOffset = if (verticalMirror) {
            (if (flags.largeSprites) 15 else 7) - (scanline - spriteY)
        } else {
            scanline - spriteY
        }

        var tileAddr = if (flags.largeSprites) {
            ((if (mTileIndex.bit0) 0x1000 else 0x0000) or (mTileIndex and 0xFE shl 4)) + (if (lineOffset >= 8) lineOffset + 8 else lineOffset)
        } else {
            ((mTileIndex shl 4) or flags.spritePatternAddr) + lineOffset
        }

        var fetchLastSprite = true

        if ((spriteIndex < spriteCount || extraSprite) && spriteY < 240) {
            val info = spriteTiles[spriteIndex]

            info.backgroundPriority = backgroundPriority
            info.horizontalMirror = horizontalMirror
            info.verticalMirror = verticalMirror
            info.paletteOffset = (attributes and 0x03 shl 2) or 0x10

            if (extraSprite) {
                // Use DebugReadVRAM for extra sprites to prevent side-effects.
                info.lowByte = console.mapper!!.debugReadVRAM(tileAddr)
                info.highByte = console.mapper!!.debugReadVRAM(tileAddr + 8)
            } else {
                fetchLastSprite = false
                info.lowByte = readVRam(tileAddr)
                info.highByte = readVRam(tileAddr + 8)
            }

            info.tileAddr = tileAddr
            // info.offsetY = lineOffset
            info.spriteX = spriteX

            if (scanline >= 0) {
                // Sprites read on prerender scanline are not shown on scanline 0
                var i = 0
                while (i < 8 && (spriteX + i + 1) < 257) {
                    hasSprite[spriteX + i + 1] = true
                    i++
                }
            }
        }

        if (fetchLastSprite) {
            // Fetches to sprite 0xFF for remaining sprites/hidden - used by MMC3 IRQ counter
            mTileIndex = 0xFF

            tileAddr = if (flags.largeSprites) {
                (if (mTileIndex.bit0) 0x1000 else 0x0000) or (mTileIndex and 0x01.inv() shl 4)
            } else {
                (mTileIndex shl 4) or flags.spritePatternAddr
            }

            readVRam(tileAddr)
            readVRam(tileAddr + 8)
        }

        spriteIndex++
    }

    private fun loadTileInfo() {
        if (renderingEnabled) {
            when (cycle and 0x07) {
                1 -> {
                    previousTilePalette = currentTilePalette
                    currentTilePalette = tile.paletteOffset

                    state.lowBitShift = state.lowBitShift or tile.lowByte
                    state.highBitShift = state.highBitShift or tile.highByte

                    val tileIndex = readVRam(nametableAddress)
                    tile.tileAddr = (tileIndex shl 4) or (state.videoRamAddr shr 12) or flags.backgroundPatternAddr
                    tile.offsetY = state.videoRamAddr shr 12
                }
                3 -> {
                    val shift = ((state.videoRamAddr shr 4) and 0x04) or (state.videoRamAddr and 0x02)
                    tile.paletteOffset = readVRam(attributeAddress) shr shift and 0x03 shl 2
                }
                5 -> {
                    tile.lowByte = readVRam(tile.tileAddr)
                }
                7 -> {
                    tile.highByte = readVRam(tile.tileAddr + 8)
                }
            }
        }
    }

    private fun updateState() {
        needStateUpdate = false

        // Rendering enabled flag is apparently set with a 1 cycle delay (i.e setting it at cycle 5 will render cycle 6 like cycle 5 and then take the new settings for cycle 7)
        if (prevRenderingEnabled != renderingEnabled) {
            prevRenderingEnabled = renderingEnabled

            if (scanline < 240) {
                if (prevRenderingEnabled) {
                    // Rendering was just enabled, perform oam corruption if any is pending
                    processOamCorruption()
                } else {
                    // Rendering was just disabled by a write to $2001, check for oam row corruption glitch
                    setOamCorruptionFlags()

                    // When rendering is disabled midscreen, set the vram bus back to the value of 'v'
                    busAddress(state.videoRamAddr and 0x3FFF)

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
                        spriteAddrH = (state.spriteRamAddr shr 2) and 0x3F
                        spriteAddrL = state.spriteRamAddr and 0x03
                    }
                }
            }
        }

        if (renderingEnabled != (flags.backgroundEnabled or flags.spritesEnabled)) {
            renderingEnabled = flags.backgroundEnabled or flags.spritesEnabled
            needStateUpdate = true
        }

        if (updateVramAddrDelay != 0) {
            updateVramAddrDelay--

            if (updateVramAddrDelay == 0) {
                if (settings.flag(ENABLE_PPU_2006_SCROLL_GLITCH) &&
                    scanline < 240 &&
                    renderingEnabled
                ) {
                    //When a $2006 address update lands on the Y or X increment, the written value is bugged and is ANDed with the incremented value
                    if (cycle == 257) {
                        state.videoRamAddr = state.videoRamAddr and updateVramAddr
                    } else if (cycle > 0 && (cycle and 0x07) == 0 && (cycle <= 256 || cycle > 320)) {
                        state.videoRamAddr = (updateVramAddr and 0x41F.inv()) or (state.videoRamAddr and updateVramAddr and 0x41F)
                    } else {
                        state.videoRamAddr = updateVramAddr
                    }
                } else {
                    state.videoRamAddr = updateVramAddr
                }

                // The glitches updates corrupt both V and T, so set the new value of V back into T
                state.tmpVideoRamAddr = state.videoRamAddr

                if (scanline >= 240 || !renderingEnabled) {
                    // Only set the VRAM address on the bus if the PPU is not rendering
                    // More info here: https://forums.nesdev.com/viewtopic.php?p=132145#p132145
                    // Trigger bus address change when setting the vram address - needed by MMC3 IRQ counter
                    // "4) Should be clocked when A12 changes to 1 via $2006 write"
                    busAddress(state.videoRamAddr and 0x3FFF)
                }
            } else {
                needStateUpdate = true
            }
        }

        if (ignoreVramRead > 0) {
            ignoreVramRead--

            if (ignoreVramRead > 0) {
                needStateUpdate = true
            }
        }

        if (needVideoRamIncrement) {
            // Delay vram address increment by 1 ppu cycle after a read/write to 2007
            // This allows the full_palette tests to properly display single-pixel glitches
            // that display the "wrong" color on the screen until the increment occurs (matches hardware)
            needVideoRamIncrement = false
            updateVideoRamAddr()
        }
    }

    private fun setOamCorruptionFlags() {
        if (!settings.flag(ENABLE_PPU_OAM_ROW_CORRUPTION)) {
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

    private fun processSpriteEvaluation() {
        if (renderingEnabled || (console.region == PAL && scanline >= palSpriteEvalScanline)) {
            if (cycle < 65) {
                // Clear secondary OAM at between cycle 1 and 64
                oamCopybuffer = 0xFF
                secondarySpriteRAM[(cycle - 1) shr 1] = 0xFF
            } else {
                if (cycle == 65) {
                    sprite0Added = false
                    spriteInRange = false
                    secondaryOAMAddr = 0

                    overflowBugCounter = 0

                    oamCopyDone = false
                    spriteAddrH = state.spriteRamAddr shr 2 and 0x3F
                    spriteAddrL = state.spriteRamAddr and 0x03

                    firstVisibleSpriteAddr = spriteAddrH * 4
                    lastVisibleSpriteAddr = firstVisibleSpriteAddr
                } else if (cycle == 256) {
                    sprite0Visible = sprite0Added
                    spriteCount = secondaryOAMAddr shr 2
                }

                if (cycle.bit0) {
                    // Read a byte from the primary OAM on odd cycles
                    oamCopybuffer = readSpriteRam(state.spriteRamAddr)
                } else {
                    if (oamCopyDone) {
                        spriteAddrH = (spriteAddrH + 1) and 0x3F

                        if (secondaryOAMAddr >= 0x20) {
                            // As seen above, a side effect of the OAM write disable signal is to turn writes to the secondary OAM into reads from it.
                            oamCopybuffer = secondarySpriteRAM[secondaryOAMAddr and 0x1F]
                        }
                    } else {
                        if (!spriteInRange &&
                            scanline >= oamCopybuffer &&
                            scanline < oamCopybuffer + if (flags.largeSprites) 16 else 8
                        ) {
                            spriteInRange = true
                        }

                        if (secondaryOAMAddr < 0x20) {
                            // Copy 1 byte to secondary OAM
                            secondarySpriteRAM[secondaryOAMAddr] = oamCopybuffer

                            if (spriteInRange) {
                                spriteAddrL++
                                secondaryOAMAddr++

                                if (spriteAddrH == 0) {
                                    sprite0Added = true
                                }

                                // Note: Using "(_secondaryOAMAddr & 0x03) == 0" instead of "_spriteAddrL == 0" is required
                                // to replicate a hardware bug noticed in oam_flicker_test_reenable when disabling & re-enabling
                                // rendering on a single scanline
                                if ((secondaryOAMAddr and 0x03) == 0) {
                                    // Done copying all 4 bytes
                                    spriteInRange = false
                                    spriteAddrL = 0
                                    lastVisibleSpriteAddr = spriteAddrH * 4
                                    spriteAddrH = (spriteAddrH + 1) and 0x3F

                                    if (spriteAddrH == 0) {
                                        oamCopyDone = true
                                    }
                                }
                            } else {
                                // Nothing to copy, skip to next sprite
                                spriteAddrH = (spriteAddrH + 1) and 0x3F

                                if (spriteAddrH == 0) {
                                    oamCopyDone = true
                                }
                            }
                        } else {
                            // As seen above, a side effect of the OAM write disable signal is to turn writes to the secondary OAM into reads from it.
                            oamCopybuffer = secondarySpriteRAM[secondaryOAMAddr and 0x1F]

                            // 8 sprites have been found, check next sprite for overflow + emulate PPU bug
                            if (spriteInRange) {
                                // Sprite is visible, consider this to be an overflow
                                statusFlags.spriteOverflow = true
                                spriteAddrL++

                                if (spriteAddrL == 4) {
                                    spriteAddrH = (spriteAddrH + 1) and 0x3F
                                    spriteAddrL = 0
                                }

                                if (overflowBugCounter == 0) {
                                    overflowBugCounter = 3
                                } else if (overflowBugCounter > 0) {
                                    overflowBugCounter--

                                    if (overflowBugCounter == 0) {
                                        // After it finishes "fetching" this sprite(and setting the overflow flag), it realigns back at the beginning of this line and then continues here on the next sprite
                                        oamCopyDone = true
                                        spriteAddrL = 0
                                    }
                                }
                            } else {
                                // Sprite isn't on this scanline, trigger sprite evaluation bug - increment both H & L at the same time
                                spriteAddrH = (spriteAddrH + 1) and 0x3F
                                spriteAddrL = (spriteAddrL + 1) and 0x03

                                if (spriteAddrH == 0) {
                                    oamCopyDone = true
                                }
                            }
                        }
                    }

                    state.spriteRamAddr = (spriteAddrL and 0x03) or (spriteAddrH shl 2)
                }
            }
        }
    }

    private fun processOamCorruption() {
        if (!settings.flag(ENABLE_PPU_OAM_ROW_CORRUPTION)) {
            return
        }

        // Copy first OAM row over another row, as needed by corruption flags
        // (can be over itself, which causes no actual harm).
        repeat(32) {
            if (corruptOamRow[it]) {
                if (it > 0) {
                    // memcpy(_spriteRAM + i * 8, _spriteRAM, 8)
                    spriteRAM.copyInto(spriteRAM, it * 8, 0, 8)
                }

                corruptOamRow[it] = false
            }
        }
    }

    fun pixelAt(x: Int, y: Int): Int {
        return (currentOutputBuffer[y shl 8 or x] and paletteRamMask) or intensifyColorBits
    }

    fun pixelBrightnessAt(x: Int, y: Int): Int {
        val pixel = pixelAt(x, y)
        val color = console.settings.palette[pixel and 0x3F]
        return color.loByte + color.hiByte + color.higherByte
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.READ, 0x2000, 0x3FFF)
        ranges.addHandler(MemoryOperation.WRITE, 0x2000, 0x3FFF)
        ranges.addHandler(MemoryOperation.WRITE, 0x4014)
    }

    private fun registerId(addr: Int): PpuRegister {
        return if (addr == 0x4014) {
            SPRITE_DMA
        } else {
            PpuRegister.ENTRIES[addr and 0x07]
        }
    }

    private fun processStatusRegOpenBus(result: Int): IntArray {
        return when (settings.ppuModel) {
            PPU_2C05A -> intArrayOf(0x00, result or 0x1B)
            PPU_2C05B -> intArrayOf(0x00, result or 0x3D)
            PPU_2C05C -> intArrayOf(0x00, result or 0x1C)
            PPU_2C05D -> intArrayOf(0x00, result or 0x1B)
            PPU_2C05E -> STATUS_REG_2C05E
            else -> STATUS_REG_NULL
        }
    }

    private fun updateStatusFlag() {
        state.status = (if (statusFlags.spriteOverflow) 0x20 else 0) or
            (if (statusFlags.sprite0Hit) 0x40 else 0) or
            (if (statusFlags.verticalBlank) 0x80 else 0)

        statusFlags.verticalBlank = false
        console.cpu.nmi = false

        if (scanline == nmiScanline && cycle == 0) {
            // Reading one PPU clock before reads it as clear and never sets the flag or generates NMI for that frame
            preventVBlankFlag = true
        }
    }

    private fun updateVideoRamAddr() {
        if (scanline >= 240 || !renderingEnabled) {
            state.videoRamAddr = (state.videoRamAddr + if (flags.verticalWrite) 32 else 1) and 0x7FFF

            // Trigger memory read when setting the vram address - needed by MMC3 IRQ counter
            // Should be clocked when A12 changes to 1 via $2007 read/write
            busAddress(state.videoRamAddr and 0x3FFF)
        } else {
            // During rendering (on the pre-render line and the visible lines 0-239, provided either background or sprite rendering is enabled),
            // it will update v in an odd way, triggering a coarse X increment and a Y increment simultaneously
            incHorizontalScrolling()
            incVerticalScrolling()
        }
    }

    private fun incHorizontalScrolling() {
        // Taken from http://wiki.nesdev.com/w/index.php/The_skinny_on_NES_scrolling#Wrapping_around
        // Increase coarse X scrolling value.
        val addr = state.videoRamAddr

        state.videoRamAddr = if ((addr and 0x001F) == 31) {
            // When the value is 31, wrap around to 0 and switch nametable
            (addr and 0x001F.inv()) xor 0x0400
        } else {
            addr + 1
        }
    }

    private fun incVerticalScrolling() {
        val addr = state.videoRamAddr

        state.videoRamAddr = if ((addr and 0x7000) != 0x7000) {
            // if fine Y < 7
            addr + 0x1000 // increment fine Y
        } else {
            // fine Y = 0
            var a = addr and 0x7000.inv()
            var y = a and 0x03E0 shr 5 // let y = coarse Y

            when (y) {
                29 -> {
                    y = 0 // coarse Y = 0
                    a = a xor 0x0800 // switch vertical nametable
                }
                31 -> {
                    y = 0 // coarse Y = 0, nametable not switched
                }
                else -> {
                    y++ // increment coarse Y
                }
            }

            // addr = (addr & ~0x03E0) | (y << 5);
            (a and 0x03E0.inv()) or (y shl 5) // put coarse Y back into v
        }
    }

    val nametableAddress
        // Taken from http://wiki.nesdev.com/w/index.php/The_skinny_on_NES_scrolling#Tile_and_attribute_fetching
        get() = 0x2000 or (state.videoRamAddr and 0x0FFF)

    val attributeAddress
        // Taken from http://wiki.nesdev.com/w/index.php/The_skinny_on_NES_scrolling#Tile_and_attribute_fetching
        get() = 0x23C0 or (state.videoRamAddr and 0x0C00) or (state.videoRamAddr shr 4 and 0x38) or (state.videoRamAddr shr 2 and 0x07)

    override fun read(addr: Int, type: MemoryOperationType): Int {
        var openBusMask = 0xFF
        var result = 0

        when (registerId(addr)) {
            STATUS -> {
                state.writeToggle = false
                updateStatusFlag()
                result = state.status
                openBusMask = 0x1F

                val (a, b) = processStatusRegOpenBus(result)

                if (a != -1) {
                    openBusMask = a
                }
                if (b != -1) {
                    result = b
                }
            }
            SPRITE_DATA -> {
                if (!settings.flag(DISABLE_PPU_2004_READS)) {
                    if (scanline <= 239 && renderingEnabled) {
                        // While the screen is begin drawn
                        if (cycle in 257..320) {
                            // If we're doing sprite rendering, set OAM copy buffer to its proper value.  This is done here for performance.
                            // It's faster to only do this here when it's needed, rather than splitting LoadSpriteTileInfo() into an 8-step process
                            val step = min((cycle - 257) % 8, 3)
                            secondaryOAMAddr = ((cycle - 257) / 8) * 4 + step
                            oamCopybuffer = secondarySpriteRAM[secondaryOAMAddr]
                        }

                        // Return the value that PPU is currently using for sprite evaluation/rendering
                        result = oamCopybuffer
                    } else {
                        result = readSpriteRam(state.spriteRamAddr)
                    }

                    openBusMask = 0x00
                }
            }
            VIDEO_MEMORY_DATA -> {
                if (ignoreVramRead != 0) {
                    // 2 reads to $2007 in quick succession (2 consecutive CPU cycles) causes the 2nd read to be ignored (normally depends on PPU/CPU timing, but this is the simplest solution)
                    // Return open bus in this case? (which will match the last value read)
                    openBusMask = 0xFF
                } else {
                    result = memoryReadBuffer
                    memoryReadBuffer = readVRam(ppuBusAddress and 0x3FFF)

                    if ((ppuBusAddress and 0x3FFF) >= 0x3F00 && !settings.flag(DISABLE_PALETTE_READ)) {
                        result = readPaletteRam(ppuBusAddress) or (openBus and 0xC0)
                        openBusMask = 0xC0
                    } else {
                        openBusMask = 0x00
                    }

                    ignoreVramRead = 6
                    needStateUpdate = true
                    needVideoRamIncrement = true
                }
            }
            else -> Unit
        }

        return applyOpenBus(openBusMask, result)
    }

    override fun peek(addr: Int): Int {
        var openBusMask = 0xFF
        var result = 0

        when (registerId(addr)) {
            STATUS -> {
                result = (if (statusFlags.spriteOverflow) 0x20 else 0x00) or
                    (if (statusFlags.sprite0Hit) 0x40 else 0x00) or
                    (if (statusFlags.verticalBlank) 0x80 else 0x00)

                if (scanline == nmiScanline && cycle < 3) {
                    // Clear vertical blank flag
                    result = result and 0x7F
                }

                openBusMask = 0x1F

                val (a, b) = processStatusRegOpenBus(result)

                if (a != -1) {
                    openBusMask = a
                }
                if (b != -1) {
                    result = b
                }
            }
            SPRITE_DATA -> {
                if (!settings.flag(DISABLE_PPU_2004_READS)) {
                    result = if (scanline <= 239 && renderingEnabled) {
                        // While the screen is begin drawn
                        if (cycle in 257..320) {
                            // If we're doing sprite rendering, set OAM copy buffer to its proper value.  This is done here for performance.
                            // It's faster to only do this here when it's needed, rather than splitting LoadSpriteTileInfo() into an 8-step process
                            val step = min((cycle - 257) % 8, 3)
                            val oamAddr = ((cycle - 257) / 8) * 4 + step
                            secondarySpriteRAM[oamAddr]
                        } else {
                            oamCopybuffer
                        }
                    } else {
                        spriteRAM[state.spriteRamAddr]
                    }

                    openBusMask = 0x00
                }
            }
            VIDEO_MEMORY_DATA -> {
                result = memoryReadBuffer

                if ((state.videoRamAddr and 0x3FFF) >= 0x3F00 && !settings.flag(DISABLE_PALETTE_READ)) {
                    //Note: When grayscale is turned on, the read values also have the grayscale mask applied to them
                    result = readPaletteRam(state.videoRamAddr) or (openBus and 0xC0)
                    openBusMask = 0xC0
                } else {
                    openBusMask = 0x00
                }
            }
            else -> Unit
        }

        return result or (openBus and openBusMask)
    }

    private inline fun applyOpenBus(mask: Int, value: Int): Int {
        openBus(mask.inv(), value)
        return value or (openBus and mask)
    }

    private fun readPaletteRam(addr: Int): Int {
        var a = addr and 0x1F

        if (a == 0x10 || a == 0x14 || a == 0x18 || a == 0x1C) {
            a = a and 0x10.inv()
        }

        return paletteRAM[a] and paletteRamMask
    }

    protected fun readVRam(addr: Int): Int {
        busAddress(addr)
        return console.mapper!!.readVRAM(addr)
    }

    protected fun writeVRam(addr: Int, value: Int) {
        busAddress(addr)
        console.mapper!!.writeVRAM(addr, value)
    }

    protected fun readSpriteRam(addr: Int): Int {
        return if (!enableOamDecay) {
            spriteRAM[addr]
        } else {
            val elapsedCycles = console.cpu.cycleCount - oamDecayCycles[addr shr 3]

            if (elapsedCycles <= OAM_DECAY_CYCLE_COUNT) {
                oamDecayCycles[addr shr 3] = console.cpu.cycleCount
                spriteRAM[addr]
            } else {
                // If this 8-byte row hasn't been read/written to in over 3000 cpu cycles (~1.7ms), return 0x10 to simulate decay
                0x10
            }
        }
    }

    protected fun writeSpriteRam(addr: Int, value: Int) {
        spriteRAM[addr] = value

        if (enableOamDecay) {
            oamDecayCycles[addr shr 3] = console.cpu.cycleCount
        }
    }

    private fun openBus(mask: Int, value: Int) {
        // Decay expired bits, set new bits and update stamps on each individual bit
        if (mask and 0xFF == 0xFF) {
            // Shortcut when mask is 0xFF - all bits are set to the value and stamps updated
            openBus = value
            openBusDecayStamp.fill(frameCount)
        } else {
            var m = mask
            var v = value

            var ob = openBus shl 8 and 0xFFFF

            for (i in 0..7) {
                ob = ob shr 1

                if (m.bit0) {
                    ob = if (v.bit0) {
                        ob or 0x80
                    } else {
                        ob and 0xFF7F
                    }

                    openBusDecayStamp[i] = frameCount
                } else if (frameCount - openBusDecayStamp[i] > 30) {
                    ob = ob and 0xFF7F
                }

                v = v shr 1
                m = m shr 1
            }

            openBus = ob
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        if (addr != 0x4014) {
            openBus(0xFF, value)
        }

        when (registerId(addr)) {
            CONTROL -> {
                if (settings.ppuModel.is2C05) {
                    setMaskRegister(value)
                } else {
                    setControlRegister(value)
                }
            }
            MASK -> {
                if (settings.ppuModel.is2C05) {
                    setControlRegister(value)
                } else {
                    setMaskRegister(value)
                }
            }
            SPRITE_ADDR -> {
                state.spriteRamAddr = value
            }
            SPRITE_DATA -> {
                if ((scanline >= 240 && (console.region != PAL || scanline < palSpriteEvalScanline)) || !renderingEnabled) {
                    if ((state.spriteRamAddr and 0x03) == 0x02) {
                        // The three unimplemented bits of each sprite's byte 2 do not exist in the PPU
                        // and always read back as 0 on PPU revisions that allow reading PPU OAM through OAMDATA ($2004)
                        writeSpriteRam(state.spriteRamAddr, value and 0xE3)
                    } else {
                        writeSpriteRam(state.spriteRamAddr, value)
                    }

                    state.spriteRamAddr = (state.spriteRamAddr + 1) and 0xFF
                } else {
                    // Writes to OAMDATA during rendering (on the pre-render line and the visible lines 0-239,
                    // provided either sprite or background rendering is enabled) do not modify values in OAM,
                    // but do perform a glitchy increment of OAMADDR, bumping only the high 6 bits
                    state.spriteRamAddr = (state.spriteRamAddr + 4) and 0xFF
                }
            }
            SCROLL_OFFSET -> {
                if (state.writeToggle) {
                    state.tmpVideoRamAddr = (state.tmpVideoRamAddr and 0x73E0.inv()) or (value and 0xF8 shl 2) or (value and 0x07 shl 12)
                } else {
                    state.xScroll = value and 0x07
                    // and ~0x001F
                    val newAddr = (state.tmpVideoRamAddr and 0x001F.inv()) or (value shr 3)
                    processTmpAddrScrollGlitch(newAddr, console.memoryManager.openBus() shr 3, 0x001F)
                }

                state.writeToggle = !state.writeToggle
            }
            VIDEO_MEMORY_ADDR -> {
                if (state.writeToggle) {
                    state.tmpVideoRamAddr = (state.tmpVideoRamAddr and 0x00FF.inv()) or value

                    // Video RAM update is apparently delayed by 3 PPU cycles (based on Visual NES findings)
                    needStateUpdate = true
                    updateVramAddrDelay = 3
                    updateVramAddr = state.tmpVideoRamAddr
                } else {
                    val newAddr = (state.tmpVideoRamAddr and 0xFF00.inv()) or (value and 0x3F shl 8)
                    processTmpAddrScrollGlitch(
                        newAddr,
                        console.memoryManager.openBus() shl 8,
                        0x0C00,
                    )
                }

                state.writeToggle = !state.writeToggle
            }
            VIDEO_MEMORY_DATA -> {
                // The palettes start at PPU address $3F00 and $3F10.
                if ((ppuBusAddress and 0x3FFF) >= 0x3F00) {
                    writePaletteRam(ppuBusAddress, value)
                } else if (scanline >= 240 || !renderingEnabled) {
                    console.mapper!!.writeVRAM(ppuBusAddress and 0x3FFF, value)
                } else {
                    // During rendering, the value written is ignored, and instead the address' LSB is used (not confirmed, based on Visual NES)
                    console.mapper!!.writeVRAM(ppuBusAddress and 0x3FFF, ppuBusAddress and 0xFF)
                }

                needStateUpdate = true
                needVideoRamIncrement = true
            }
            SPRITE_DMA -> {
                console.cpu.runDmaTransfer(value)
            }
            else -> Unit
        }
    }

    private fun setMaskRegister(value: Int) {
        state.mask = value

        flags.grayscale = value.bit0
        flags.backgroundMask = value.bit1
        flags.spriteMask = value.bit2
        flags.backgroundEnabled = value.bit3
        flags.spritesEnabled = value.bit4
        flags.intensifyBlue = value.bit7

        if (console.region == NTSC) {
            flags.intensifyRed = value.bit5
            flags.intensifyGreen = value.bit6
        } else {
            // Note that on the Dendy and PAL NES, the green and red bits swap meaning.
            flags.intensifyRed = value.bit6
            flags.intensifyGreen = value.bit5
        }

        if (renderingEnabled != (flags.backgroundEnabled or flags.spritesEnabled)) {
            needStateUpdate = true
        }

        updateMinimumDrawCycles()
        updateGrayscaleAndIntensifyBits()

        //"Bit 0 controls a greyscale mode, which causes the palette to use only the colors from the grey column: $00, $10, $20, $30.
        // This is implemented as a bitwise AND with $30 on any value read from PPU $3F00-$3FFF"
        paletteRamMask = if (flags.grayscale) 0x30 else 0x3F
    }

    private fun setControlRegister(value: Int) {
        state.control = value and 0xFC

        val nameTable = value and 0x03
        val addr = (state.tmpVideoRamAddr and 0x0C00.inv()) or (nameTable shl 10)
        processTmpAddrScrollGlitch(addr, console.memoryManager.openBus() shl 10, 0x0400)

        flags.verticalWrite = value.bit2
        flags.spritePatternAddr = if (value.bit3) 0x1000 else 0x0000
        flags.backgroundPatternAddr = if (value.bit4) 0x1000 else 0x0000
        flags.largeSprites = value.bit5
        flags.vBlank = value.bit7

        // By toggling NMI_output ($2000 bit 7) during vertical blank without reading $2002, a program can cause /NMI to be pulled low multiple times, causing multiple NMIs to be generated.
        if (!flags.vBlank) {
            console.cpu.nmi = false
        } else if (statusFlags.verticalBlank) {
            console.cpu.nmi = true
        }
    }

    private inline fun busAddress(addr: Int) {
        ppuBusAddress = addr
        console.mapper!!.notifyVRAMAddressChange(addr)
    }

    private fun processTmpAddrScrollGlitch(addr: Int, value: Int, mask: Int) {
        state.tmpVideoRamAddr = addr

        if (cycle == 257 && settings.flag(ENABLE_PPU_2000_SCROLL_GLITCH) &&
            scanline < 240 &&
            renderingEnabled
        ) {
            // Use open bus to set some parts of V (glitch that occurs when writing to $2000/$2005/$2006 on cycle 257)
            state.videoRamAddr = (state.videoRamAddr and mask.inv()) or (value and mask)
        }
    }

    private fun writePaletteRam(addr: Int, value: Int) {
        val a = addr and 0x1F
        val b = value and 0x3F

        when (a) {
            0x00, 0x10 -> {
                paletteRAM[0x00] = b
                paletteRAM[0x10] = b
            }
            0x04, 0x14 -> {
                paletteRAM[0x04] = b
                paletteRAM[0x14] = b
            }
            0x08, 0x18 -> {
                paletteRAM[0x08] = b
                paletteRAM[0x18] = b
            }
            0x0C, 0x1C -> {
                paletteRAM[0x0C] = b
                paletteRAM[0x1C] = b
            }
            else -> {
                paletteRAM[a] = b
            }
        }
    }

    override fun saveState(s: Snapshot) {
        val disablePpu2004Reads = console.settings.flag(DISABLE_PPU_2004_READS)
        val disablePaletteRead = console.settings.flag(DISABLE_PALETTE_READ)
        val disableOamAddrBug = console.settings.flag(DISABLE_OAM_ADDR_BUG)

        s.write("state", state)
        s.write("flags", flags)
        s.write("paletteRamMask", paletteRamMask)
        s.write("intensifyColorBits", intensifyColorBits)
        s.write("statusFlags", statusFlags)
        s.write("scanline", scanline)
        s.write("cycle", cycle)
        s.write("frameCount", frameCount)
        s.write("memoryReadBuffer", memoryReadBuffer)
        s.write("previousTilePalette", previousTilePalette)
        s.write("currentTilePalette", currentTilePalette)
        s.write("tile", tile)
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
        s.write("prevIsRenderingEnabled", prevRenderingEnabled)
        s.write("isRenderingEnabled", renderingEnabled)
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
        s.write("needStateUpdate", needStateUpdate)
        s.write("ppuBusAddress", ppuBusAddress)
        s.write("preventVBlankFlag", preventVBlankFlag)
        s.write("masterClock", masterClock)
        s.write("needVideoRamIncrement", needVideoRamIncrement)
        repeat(spriteTiles.size) { s.write("spriteTile$it", spriteTiles[it]) }
    }

    override fun restoreState(s: Snapshot) {
        s.readSnapshotable("state", state)
        s.readSnapshotable("flags", flags)
        paletteRamMask = s.readInt("paletteRamMask")
        intensifyColorBits = s.readInt("intensifyColorBits")
        s.readSnapshotable("statusFlags", statusFlags)
        scanline = s.readInt("scanline")
        cycle = s.readInt("cycle")
        frameCount = s.readInt("frameCount")
        memoryReadBuffer = s.readInt("memoryReadBuffer")
        previousTilePalette = s.readInt("previousTilePalette")
        currentTilePalette = s.readInt("currentTilePalette")
        s.readSnapshotable("tile", tile)
        spriteIndex = s.readInt("spriteIndex")
        spriteCount = s.readInt("spriteCount")
        secondaryOAMAddr = s.readInt("secondaryOAMAddr")
        sprite0Visible = s.readBoolean("sprite0Visible")
        oamCopybuffer = s.readInt("oamCopybuffer")
        spriteInRange = s.readBoolean("spriteInRange")
        sprite0Added = s.readBoolean("sprite0Added")
        spriteAddrH = s.readInt("spriteAddrH")
        spriteAddrL = s.readInt("spriteAddrL")
        oamCopyDone = s.readBoolean("oamCopyDone")
        prevRenderingEnabled = s.readBoolean("prevIsRenderingEnabled")
        renderingEnabled = s.readBoolean("isRenderingEnabled")
        openBus = s.readInt("openBus")
        ignoreVramRead = s.readInt("ignoreVramRead")
        needVideoRamIncrement = s.readBoolean("needVideoRamIncrement")
        s.readIntArray("paletteRAM", paletteRAM)
        s.readIntArray("spriteRAM", spriteRAM)
        s.readIntArray("secondarySpriteRAM", secondarySpriteRAM)
        s.readIntArray("openBusDecayStamp", openBusDecayStamp)
        val disablePpu2004Reads = s.readBoolean("disablePpu2004Reads")
        val disablePaletteRead = s.readBoolean("disablePaletteRead")
        val disableOamAddrBug = s.readBoolean("disableOamAddrBug")
        overflowBugCounter = s.readInt("overflowBugCounter")
        updateVramAddr = s.readInt("updateVramAddr")
        updateVramAddrDelay = s.readInt("updateVramAddrDelay")
        needStateUpdate = s.readBoolean("needStateUpdate")
        ppuBusAddress = s.readInt("ppuBusAddress")
        preventVBlankFlag = s.readBoolean("preventVBlankFlag")
        masterClock = s.readLong("masterClock")
        repeat(spriteTiles.size) { s.readSnapshotable("spriteTile$it", spriteTiles[it]) }

        console.settings.flag(DISABLE_PPU_2004_READS, disablePpu2004Reads)
        console.settings.flag(DISABLE_PALETTE_READ, disablePaletteRead)
        console.settings.flag(DISABLE_OAM_ADDR_BUG, disableOamAddrBug)

        updateRegion(console.region)
        updateMinimumDrawCycles()
        updateGrayscaleAndIntensifyBits()

        // Set oam decay cycle to the current cycle to ensure it doesn't
        // decay when loading a state.
        oamDecayCycles.fill(console.cpu.cycleCount)

        corruptOamRow.fill(false)
        hasSprite.fill(false)

        lastUpdatedPixel = -1

        updateApuStatus()
        updateRegion(console.region)
    }

    fun screenBuffer(previous: Boolean) = if (previous) {
        if (currentOutputBuffer === outputBuffers[0]) outputBuffers[1]
        else outputBuffers[0]
    } else {
        currentOutputBuffer
    }

    private inline fun processPpuCycle() {
        // console.debugger.processPpuCycle()
    }

    companion object {

        const val SCREEN_WIDTH = 256
        const val SCREEN_HEIGHT = 240
        const val PIXEL_COUNT = SCREEN_WIDTH * SCREEN_HEIGHT
        const val OAM_DECAY_CYCLE_COUNT = 3000L

        @JvmStatic private val STATUS_REG_2C05E = intArrayOf(0x00, -1)
        @JvmStatic private val STATUS_REG_NULL = intArrayOf(-1, -1)

        @JvmStatic val PALETTE_BOOT_RAM = intArrayOf(
            0x09, 0x01, 0x00, 0x01, 0x00, 0x02, 0x02, 0x0D, //
            0x08, 0x10, 0x08, 0x24, 0x00, 0x00, 0x04, 0x2C, //
            0x09, 0x01, 0x34, 0x03, 0x00, 0x04, 0x00, 0x14, //
            0x08, 0x3A, 0x00, 0x02, 0x00, 0x20, 0x2C, 0x08, //
        )
    }
}
