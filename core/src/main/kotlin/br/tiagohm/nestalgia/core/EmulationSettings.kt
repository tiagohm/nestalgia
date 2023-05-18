package br.tiagohm.nestalgia.core

import kotlin.math.min

@Suppress("NOTHING_TO_INLINE", "DuplicatedCode")
class EmulationSettings : Snapshotable {

    @PublishedApi
    internal var flags = 0UL

    // Zapper
    @JvmField
    val zapperDetectionRadius = IntArray(ControlDevice.PORT_COUNT)

    // Ascii Turbo File II
    @JvmField
    var asciiTurboFileSlot = 0

    // Console
    @JvmField
    var region = Region.AUTO

    @JvmField
    var ramPowerOnState = RamPowerOnState.ALL_ZEROS

    @JvmField
    var dipSwitches = 0

    // Devices
    private val controllerTypes = Array(ControlDevice.PORT_COUNT) { ControllerType.NONE }
    private val controllerKeys = Array(ControlDevice.PORT_COUNT) { KeyMapping.NONE }
    private var isNeedControllerUpdate = false

    @JvmField
    var isKeyboardMode = true

    var expansionDevice = ExpansionPortDevice.NONE
        set(value) {
            field = value
            isNeedControllerUpdate = true
        }

    var consoleType = ConsoleType.NES
        set(value) {
            field = value
            isNeedControllerUpdate = true
        }

    // CPU
    private var emulationSpeed = 100
    private var turboSpeed = 300
    private var rewindSpeed = 100

    // APU
    private var isNeedAudioSettingsUpdate = false

    var sampleRate = 48000
        set(value) {
            if (value != field) {
                field = value
                isNeedAudioSettingsUpdate = true
            }
        }

    // PPU
    @JvmField
    var inputPollScanline = 241

    @JvmField
    var disableOverclocking = false

    val palette = UIntArray(512)

    var extraScanlinesBeforeNmi = 0
        get() = if (disableOverclocking) 0 else field

    var extraScanlinesAfterNmi = 0
        get() = if (disableOverclocking) 0 else field

    var isBackgroundEnabled = true
        private set

    var spritesEnabled = true
        private set

    var ppuModel = PpuModel.PPU_2C02
        set(value) {
            field = value
            updateCurrentPalette()
        }

    var paletteType = PaletteType.DEFAULT
        set(value) {
            if (field != value) {
                field = value
                updatePalette(value)
            }
        }

    override fun saveState(s: Snapshot) {
        s.write("flags", flags)
        s.write("zapperDetectionRadius", zapperDetectionRadius)
        s.write("asciiTurboFileSlot", asciiTurboFileSlot)
        s.write("region", region)
        s.write("ramPowerOnState", ramPowerOnState)
        s.write("dipSwitches", dipSwitches)
        s.write("controllerTypes", controllerTypes)
        controllerKeys.forEachIndexed { i, km -> s.write("controllerKeys$i", km.toSnapshot()) }
        s.write("isNeedControllerUpdate", isNeedControllerUpdate)
        s.write("isKeyboardMode", isKeyboardMode)
        s.write("expansionDevice", expansionDevice)
        s.write("consoleType", consoleType)
        s.write("emulationSpeed", emulationSpeed)
        s.write("turboSpeed", turboSpeed)
        s.write("rewindSpeed", rewindSpeed)
        s.write("isNeedAudioSettingsUpdate", isNeedAudioSettingsUpdate)
        s.write("sampleRate", sampleRate)
        s.write("inputPollScanline", inputPollScanline)
        s.write("disableOverclocking", disableOverclocking)
        s.write("extraScanlinesBeforeNmi", extraScanlinesBeforeNmi)
        s.write("extraScanlinesAfterNmi", extraScanlinesAfterNmi)
        s.write("ppuModel", ppuModel)
        s.write("paletteType", paletteType)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        isNeedControllerUpdate = s.readBoolean("isNeedControllerUpdate") ?: false
        isNeedAudioSettingsUpdate = s.readBoolean("isNeedAudioSettingsUpdate") ?: false
        flags = s.readULong("flags") ?: 0UL
        s.readIntArray("zapperDetectionRadius")?.copyInto(zapperDetectionRadius)
        asciiTurboFileSlot = s.readInt("asciiTurboFileSlot") ?: 0
        region = s.readEnum<Region>("region") ?: Region.AUTO
        ramPowerOnState = s.readEnum<RamPowerOnState>("ramPowerOnState") ?: RamPowerOnState.ALL_ZEROS
        dipSwitches = s.readInt("dipSwitches") ?: 0
        s.readEnumArray<ControllerType>("controllerTypes")?.copyInto(controllerTypes)
        for (i in 0 until ControlDevice.PORT_COUNT) controllerKeys[i] =
            s.readSnapshot("controllerKeys$i")?.let { KeyMapping.load(it) } ?: KeyMapping.NONE
        isKeyboardMode = s.readBoolean("isKeyboardMode") ?: false
        expansionDevice = s.readEnum<ExpansionPortDevice>("expansionDevice") ?: ExpansionPortDevice.NONE
        consoleType = s.readEnum<ConsoleType>("consoleType") ?: ConsoleType.NES
        emulationSpeed = s.readInt("emulationSpeed") ?: 100
        turboSpeed = s.readInt("turboSpeed") ?: 300
        rewindSpeed = s.readInt("rewindSpeed") ?: 100
        sampleRate = s.readInt("sampleRate") ?: 48000
        inputPollScanline = s.readInt("inputPollScanline") ?: 241
        disableOverclocking = s.readBoolean("disableOverclocking") ?: false
        extraScanlinesBeforeNmi = s.readInt("extraScanlinesBeforeNmi") ?: 0
        extraScanlinesAfterNmi = s.readInt("extraScanlinesAfterNmi") ?: 0
        ppuModel = s.readEnum<PpuModel>("ppuModel") ?: PpuModel.PPU_2C02
        paletteType = s.readEnum<PaletteType>("paletteType") ?: PaletteType.DEFAULT
        isBackgroundEnabled = !checkFlag(EmulationFlag.DISABLE_BACKGROUND)
        spritesEnabled = !checkFlag(EmulationFlag.DISABLE_SPRITES)
    }

    fun needControllerUpdate(): Boolean {
        return if (isNeedControllerUpdate) {
            isNeedControllerUpdate = false
            true
        } else {
            false
        }
    }

    fun setFlag(flag: EmulationFlag) {
        if ((this.flags and flag.code) == 0UL) {
            this.flags = this.flags or flag.code

            when (flag) {
                EmulationFlag.DISABLE_BACKGROUND -> isBackgroundEnabled = true
                EmulationFlag.DISABLE_SPRITES -> spritesEnabled = true
                EmulationFlag.USE_CUSTOM_VS_PALETTE -> updateCurrentPalette()
                else -> Unit
            }
        }
    }

    fun clearFlag(flag: EmulationFlag) {
        if ((this.flags and flag.code) != 0UL) {
            this.flags = this.flags and flag.code.inv()

            when (flag) {
                EmulationFlag.DISABLE_BACKGROUND -> isBackgroundEnabled = false
                EmulationFlag.DISABLE_SPRITES -> spritesEnabled = false
                EmulationFlag.USE_CUSTOM_VS_PALETTE -> updateCurrentPalette()
                else -> Unit
            }
        }
    }

    inline fun checkFlag(flag: EmulationFlag) = (this.flags and flag.code) == flag.code

    fun getEmulationSpeed(ignoreTurbo: Boolean = true): Int {
        return when {
            ignoreTurbo -> emulationSpeed
            checkFlag(EmulationFlag.FORCE_MAX_SPEED) -> 0
            checkFlag(EmulationFlag.TURBO) -> turboSpeed
            checkFlag(EmulationFlag.REWIND) -> rewindSpeed
            else -> emulationSpeed
        }
    }

    fun setEmulationSpeed(speed: Int) {
        if (speed != emulationSpeed) {
            emulationSpeed = speed
            isNeedAudioSettingsUpdate = true
        }
    }

    fun updateCurrentPalette() {
        updatePalette(paletteType)
    }

    private fun updatePalette(palette: Palette) {
        if (palette.size != 64 && palette.size != 512) {
            throw IllegalArgumentException("Invalid palette buffer size")
        }

        val data = UIntArray(512)
        palette.data.copyInto(data)

        if (palette.size == 64) {
            generateFullColorPalette(data)
        }

        updateCurrentPalette(data)
    }

    private fun updateCurrentPalette(palette: UIntArray) {
        when {
            checkFlag(EmulationFlag.USE_CUSTOM_VS_PALETTE) -> {
                for (i in 0 until 64) {
                    for (j in 0 until 8) {
                        this.palette[(j shl 6) or i] =
                            palette[(j shl 6) or PALETTE_LUT[ppuModel.ordinal][i].toInt()]
                    }
                }
            }
            ppuModel == PpuModel.PPU_2C02 -> {
                palette.copyInto(this.palette)
            }
            else -> {
                PPU_PALETTE_ARGB[ppuModel.ordinal].copyInto(this.palette, 0, 0, 64)
                generateFullColorPalette(this.palette)
            }
        }
    }

    private fun generateFullColorPalette(palette: UIntArray) {
        for (i in 0 until 64) {
            for (j in 0 until 8) {
                var redColor = (palette[i] shr 16).toUByte().toDouble()
                var greenColor = (palette[i] shr 8).toUByte().toDouble()
                var blueColor = palette[i].toUByte().toDouble()

                if ((j and 0x01) == 0x01) {
                    redColor *= 1.1
                    greenColor *= 0.9
                    blueColor *= 0.9
                }
                if ((j and 0x02) == 0x02) {
                    redColor *= 0.9
                    greenColor *= 1.1
                    blueColor *= 0.9
                }
                if ((j and 0x04) == 0x04) {
                    redColor *= 0.9
                    greenColor *= 0.9
                    blueColor *= 1.1
                }

                val r = min(255.0, redColor).toUInt()
                val g = min(255.0, greenColor).toUInt()
                val b = min(255.0, blueColor).toUInt()
                val color = 0xFF000000U or (r shl 16) or (g shl 8) or b
                palette[(j shl 6) or i] = color
            }
        }
    }

    fun initializeInputDevices(inputType: GameInputType, gameSystem: GameSystem) {
        var system = gameSystem
        var expansionDevice = ExpansionPortDevice.NONE

        val controllers = arrayOf(
            ControllerType.STANDARD,
            ControllerType.STANDARD,
            ControllerType.NONE,
            ControllerType.NONE
        )

        clearFlag(EmulationFlag.HAS_FOUR_SCORE)

        var isFamicom = system == GameSystem.FAMICOM || system == GameSystem.FDS || system == GameSystem.DENDY

        if (inputType == GameInputType.VS_ZAPPER) {
            // VS Duck Hunt, etc. need the zapper in the first port
            controllers[0] = ControllerType.VS_ZAPPER
        } else if (inputType == GameInputType.ZAPPER) {
            if (isFamicom) {
                expansionDevice = ExpansionPortDevice.ZAPPER
            } else {
                controllers[1] = ControllerType.ZAPPER
            }
        } else if (inputType == GameInputType.FOUR_SCORE) {
            setFlag(EmulationFlag.HAS_FOUR_SCORE)
            controllers[2] = ControllerType.STANDARD
            controllers[3] = ControllerType.STANDARD
        } else if (inputType == GameInputType.FOUR_PLAYER_ADAPTER) {
            setFlag(EmulationFlag.HAS_FOUR_SCORE)
            expansionDevice = ExpansionPortDevice.FOUR_PLAYER_ADAPTER
            controllers[2] = ControllerType.STANDARD
            controllers[3] = ControllerType.STANDARD
        } else if (inputType == GameInputType.ARKANOID_CONTROLLER_FAMICOM) {
            expansionDevice = ExpansionPortDevice.ARKANOID
        } else if (inputType == GameInputType.ARKANOID_CONTROLLER_NES) {
            controllers[1] = ControllerType.ARKANOID
        } else if (inputType == GameInputType.DOUBLE_ARKANOID_CONTROLLER) {
            controllers[0] = ControllerType.ARKANOID
            controllers[1] = ControllerType.ARKANOID
        } else if (inputType == GameInputType.OEKA_KIDS_TABLET) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.OEKA_KIDS_TABLET
        } else if (inputType == GameInputType.KONAMI_HYPER_SHOT) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.KONAMI_HYPER_SHOT
        } else if (inputType == GameInputType.FAMILY_BASIC_KEYBOARD) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.FAMILY_BASIC_KEYBOARD
        } else if (inputType == GameInputType.PARTY_TAP) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.PARTY_TAP
        } else if (inputType == GameInputType.PACHINKO_CONTROLLER) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.PACHINKO
        } else if (inputType == GameInputType.EXCITING_BOXING) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.EXCITING_BOXING
        } else if (inputType == GameInputType.SUBOR_KEYBOARD) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.SUBOR_KEYBOARD
            controllers[1] = ControllerType.SUBOR_MOUSE
        } else if (inputType == GameInputType.JISSEN_MAHJONG) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.JISSEN_MAHJONG
        } else if (inputType == GameInputType.BARCODE_BATTLER) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.BARCODE_BATTLER
        } else if (inputType == GameInputType.BANDAI_HYPER_SHOT) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.BANDAI_HYPER_SHOT
        } else if (inputType == GameInputType.BATTLE_BOX) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.BATTLE_BOX
        } else if (inputType == GameInputType.TURBO_FILE) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.ASCII_TURBO_FILE
        } else if (inputType == GameInputType.FAMILY_TRAINER_SIDE_A || inputType == GameInputType.FAMILY_TRAINER_SIDE_B) {
            system = GameSystem.FAMICOM
            expansionDevice = ExpansionPortDevice.FAMILY_TRAINER_MAT
        } else if (inputType == GameInputType.POWER_PAD_SIDE_A || inputType == GameInputType.POWER_PAD_SIDE_B) {
            system = GameSystem.NTSC
            controllers[1] = ControllerType.POWER_PAD
        } else if (inputType == GameInputType.SNES_CONTROLLERS) {
            controllers[0] = ControllerType.SNES
            controllers[1] = ControllerType.SNES
        }

        isFamicom = system == GameSystem.FAMICOM || system == GameSystem.FDS || system == GameSystem.DENDY

        consoleType = if (isFamicom) ConsoleType.FAMICOM else ConsoleType.NES
        for (i in 0..3) setControllerType(i, controllers[i])
        this.expansionDevice = expansionDevice
    }

    fun setControllerType(index: Int, controllerType: ControllerType) {
        controllerTypes[index] = controllerType
        isNeedControllerUpdate = true
    }

    fun getControllerType(port: Int) = controllerTypes[port]

    fun getControllerKeys(port: Int) = controllerKeys[port]

    fun setControllerKeys(port: Int, keys: KeyMapping) {
        controllerKeys[port] = keys
        isNeedControllerUpdate = true
    }

    inline val needsPause
        get() = checkFlag(EmulationFlag.PAUSED)

    inline val isInputEnabled
        get() = !checkFlag(EmulationFlag.IN_BACKGROUND) || checkFlag(EmulationFlag.ALLOW_BACKGROUND_INPUT)

    fun needAudioSettingsUpdate(): Boolean {
        val value = isNeedAudioSettingsUpdate
        if (value) isNeedControllerUpdate = false
        return value
    }

    val hasZapper
        get() = controllerTypes[0] == ControllerType.ZAPPER ||
            controllerTypes[1] == ControllerType.ZAPPER ||
            (consoleType == ConsoleType.FAMICOM && expansionDevice == ExpansionPortDevice.ZAPPER)

    val hasFourScore
        get() = consoleType == ConsoleType.FAMICOM && expansionDevice == ExpansionPortDevice.FOUR_PLAYER_ADAPTER

    companion object {
        // @formatter:off

        @JvmStatic private val PALETTE_LUT = arrayOf(
            /* 2C02 */      ubyteArrayOf(0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U, 8U, 9U, 10U, 11U, 12U, 13U, 14U, 15U, 16U, 17U, 18U, 19U, 20U, 21U, 22U, 23U, 24U, 25U, 26U, 27U, 28U, 29U, 30U, 31U, 32U, 33U, 34U, 35U, 36U, 37U, 38U, 39U, 40U, 41U, 42U, 43U, 44U, 45U, 46U, 47U, 48U, 49U, 50U, 51U, 52U, 53U, 54U, 55U, 56U, 57U, 58U, 59U, 60U, 61U, 62U, 63U),
            /* 2C03 */      ubyteArrayOf(0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U, 8U, 9U, 10U, 11U, 12U, 13U, 14U, 15U, 16U, 17U, 18U, 19U, 20U, 21U, 22U, 23U, 24U, 25U, 26U, 27U, 28U, 29U, 30U, 31U, 32U, 33U, 34U, 35U, 36U, 37U, 38U, 39U, 40U, 41U, 42U, 43U, 44U, 15U, 46U, 47U, 48U, 49U, 50U, 51U, 52U, 53U, 54U, 55U, 56U, 57U, 58U, 59U, 60U, 15U, 62U, 63U),
            /* 2C04-0001 */ ubyteArrayOf(53U, 35U, 22U, 34U, 28U, 9U, 29U, 21U, 32U, 0U, 39U, 5U, 4U, 40U, 8U, 32U, 33U, 62U, 31U, 41U, 60U, 50U, 54U, 18U, 63U, 43U, 46U, 30U, 61U, 45U, 36U, 1U, 14U, 49U, 51U, 42U, 44U, 12U, 27U, 20U, 46U, 7U, 52U, 6U, 19U, 2U, 38U, 46U, 46U, 25U, 16U, 10U, 57U, 3U, 55U, 23U, 15U, 17U, 11U, 13U, 56U, 37U, 24U, 58U),
            /* 2C04-0002 */ ubyteArrayOf(46U, 39U, 24U, 57U, 58U, 37U, 28U, 49U, 22U, 19U, 56U, 52U, 32U, 35U, 60U, 11U, 15U, 33U, 6U, 61U, 27U, 41U, 30U, 34U, 29U, 36U, 14U, 43U, 50U, 8U, 46U, 3U, 4U, 54U, 38U, 51U, 17U, 31U, 16U, 2U, 20U, 63U, 0U, 9U, 18U, 46U, 40U, 32U, 62U, 13U, 42U, 23U, 12U, 1U, 21U, 25U, 46U, 44U, 7U, 55U, 53U, 5U, 10U, 45U),
            /* 2C04-0003 */ ubyteArrayOf(20U, 37U, 58U, 16U, 11U, 32U, 49U, 9U, 1U, 46U, 54U, 8U, 21U, 61U, 62U, 60U, 34U, 28U, 5U, 18U, 25U, 24U, 23U, 27U, 0U, 3U, 46U, 2U, 22U, 6U, 52U, 53U, 35U, 15U, 14U, 55U, 13U, 39U, 38U, 32U, 41U, 4U, 33U, 36U, 17U, 45U, 46U, 31U, 44U, 30U, 57U, 51U, 7U, 42U, 40U, 29U, 10U, 46U, 50U, 56U, 19U, 43U, 63U, 12U),
            /* 2C04-0004 */ ubyteArrayOf(24U, 3U, 28U, 40U, 46U, 53U, 1U, 23U, 16U, 31U, 42U, 14U, 54U, 55U, 11U, 57U, 37U, 30U, 18U, 52U, 46U, 29U, 6U, 38U, 62U, 27U, 34U, 25U, 4U, 46U, 58U, 33U, 5U, 10U, 7U, 2U, 19U, 20U, 0U, 21U, 12U, 61U, 17U, 15U, 13U, 56U, 45U, 36U, 51U, 32U, 8U, 22U, 63U, 43U, 32U, 60U, 46U, 39U, 35U, 49U, 41U, 50U, 44U, 9U),
            /* 2C05-01 */   ubyteArrayOf(0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U, 8U, 9U, 10U, 11U, 12U, 13U, 14U, 15U, 16U, 17U, 18U, 19U, 20U, 21U, 22U, 23U, 24U, 25U, 26U, 27U, 28U, 29U, 30U, 31U, 32U, 33U, 34U, 35U, 36U, 37U, 38U, 39U, 40U, 41U, 42U, 43U, 44U, 15U, 46U, 47U, 48U, 49U, 50U, 51U, 52U, 53U, 54U, 55U, 56U, 57U, 58U, 59U, 60U, 15U, 62U, 63U),
            /* 2C05-02 */   ubyteArrayOf(0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U, 8U, 9U, 10U, 11U, 12U, 13U, 14U, 15U, 16U, 17U, 18U, 19U, 20U, 21U, 22U, 23U, 24U, 25U, 26U, 27U, 28U, 29U, 30U, 31U, 32U, 33U, 34U, 35U, 36U, 37U, 38U, 39U, 40U, 41U, 42U, 43U, 44U, 15U, 46U, 47U, 48U, 49U, 50U, 51U, 52U, 53U, 54U, 55U, 56U, 57U, 58U, 59U, 60U, 15U, 62U, 63U),
            /* 2C05-03 */   ubyteArrayOf(0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U, 8U, 9U, 10U, 11U, 12U, 13U, 14U, 15U, 16U, 17U, 18U, 19U, 20U, 21U, 22U, 23U, 24U, 25U, 26U, 27U, 28U, 29U, 30U, 31U, 32U, 33U, 34U, 35U, 36U, 37U, 38U, 39U, 40U, 41U, 42U, 43U, 44U, 15U, 46U, 47U, 48U, 49U, 50U, 51U, 52U, 53U, 54U, 55U, 56U, 57U, 58U, 59U, 60U, 15U, 62U, 63U),
            /* 2C05-04 */   ubyteArrayOf(0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U, 8U, 9U, 10U, 11U, 12U, 13U, 14U, 15U, 16U, 17U, 18U, 19U, 20U, 21U, 22U, 23U, 24U, 25U, 26U, 27U, 28U, 29U, 30U, 31U, 32U, 33U, 34U, 35U, 36U, 37U, 38U, 39U, 40U, 41U, 42U, 43U, 44U, 15U, 46U, 47U, 48U, 49U, 50U, 51U, 52U, 53U, 54U, 55U, 56U, 57U, 58U, 59U, 60U, 15U, 62U, 63U),
            /* 2C05-05 */   ubyteArrayOf(0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U, 8U, 9U, 10U, 11U, 12U, 13U, 14U, 15U, 16U, 17U, 18U, 19U, 20U, 21U, 22U, 23U, 24U, 25U, 26U, 27U, 28U, 29U, 30U, 31U, 32U, 33U, 34U, 35U, 36U, 37U, 38U, 39U, 40U, 41U, 42U, 43U, 44U, 15U, 46U, 47U, 48U, 49U, 50U, 51U, 52U, 53U, 54U, 55U, 56U, 57U, 58U, 59U, 60U, 15U, 62U, 63U),
        )

        @JvmStatic private val PPU_PALETTE_ARGB = arrayOf(
            /* 2C02 */         uintArrayOf(0xFF666666U, 0xFF002A88U, 0xFF1412A7U, 0xFF3B00A4U, 0xFF5C007EU, 0xFF6E0040U, 0xFF6C0600U, 0xFF561D00U, 0xFF333500U, 0xFF0B4800U, 0xFF005200U, 0xFF004F08U, 0xFF00404DU, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFADADADU, 0xFF155FD9U, 0xFF4240FFU, 0xFF7527FEU, 0xFFA01ACCU, 0xFFB71E7BU, 0xFFB53120U, 0xFF994E00U, 0xFF6B6D00U, 0xFF388700U, 0xFF0C9300U, 0xFF008F32U, 0xFF007C8DU, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFEFFU, 0xFF64B0FFU, 0xFF9290FFU, 0xFFC676FFU, 0xFFF36AFFU, 0xFFFE6ECCU, 0xFFFE8170U, 0xFFEA9E22U, 0xFFBCBE00U, 0xFF88D800U, 0xFF5CE430U, 0xFF45E082U, 0xFF48CDDEU, 0xFF4F4F4FU, 0xFF000000U, 0xFF000000U, 0xFFFFFEFFU, 0xFFC0DFFFU, 0xFFD3D2FFU, 0xFFE8C8FFU, 0xFFFBC2FFU, 0xFFFEC4EAU, 0xFFFECCC5U, 0xFFF7D8A5U, 0xFFE4E594U, 0xFFCFEF96U, 0xFFBDF4ABU, 0xFFB3F3CCU, 0xFFB5EBF2U, 0xFFB8B8B8U, 0xFF000000U, 0xFF000000U),
            /* 2C03 */         uintArrayOf(0xFF6D6D6DU, 0xFF002491U, 0xFF0000DAU, 0xFF6D48DAU, 0xFF91006DU, 0xFFB6006DU, 0xFFB62400U, 0xFF914800U, 0xFF6D4800U, 0xFF244800U, 0xFF006D24U, 0xFF009100U, 0xFF004848U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFB6B6B6U, 0xFF006DDAU, 0xFF0048FFU, 0xFF9100FFU, 0xFFB600FFU, 0xFFFF0091U, 0xFFFF0000U, 0xFFDA6D00U, 0xFF916D00U, 0xFF249100U, 0xFF009100U, 0xFF00B66DU, 0xFF009191U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFF6DB6FFU, 0xFF9191FFU, 0xFFDA6DFFU, 0xFFFF00FFU, 0xFFFF6DFFU, 0xFFFF9100U, 0xFFFFB600U, 0xFFDADA00U, 0xFF6DDA00U, 0xFF00FF00U, 0xFF48FFDAU, 0xFF00FFFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFFB6DAFFU, 0xFFDAB6FFU, 0xFFFFB6FFU, 0xFFFF91FFU, 0xFFFFB6B6U, 0xFFFFDA91U, 0xFFFFFF48U, 0xFFFFFF6DU, 0xFFB6FF48U, 0xFF91FF6DU, 0xFF48FFDAU, 0xFF91DAFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U),
            /* 2C04-0001 */    uintArrayOf(0xFFFFB6B6U, 0xFFDA6DFFU, 0xFFFF0000U, 0xFF9191FFU, 0xFF009191U, 0xFF244800U, 0xFF484848U, 0xFFFF0091U, 0xFFFFFFFFU, 0xFF6D6D6DU, 0xFFFFB600U, 0xFFB6006DU, 0xFF91006DU, 0xFFDADA00U, 0xFF6D4800U, 0xFFFFFFFFU, 0xFF6DB6FFU, 0xFFDAB66DU, 0xFF6D2400U, 0xFF6DDA00U, 0xFF91DAFFU, 0xFFDAB6FFU, 0xFFFFDA91U, 0xFF0048FFU, 0xFFFFDA00U, 0xFF48FFDAU, 0xFF000000U, 0xFF480000U, 0xFFDADADAU, 0xFF919191U, 0xFFFF00FFU, 0xFF002491U, 0xFF00006DU, 0xFFB6DAFFU, 0xFFFFB6FFU, 0xFF00FF00U, 0xFF00FFFFU, 0xFF004848U, 0xFF00B66DU, 0xFFB600FFU, 0xFF000000U, 0xFF914800U, 0xFFFF91FFU, 0xFFB62400U, 0xFF9100FFU, 0xFF0000DAU, 0xFFFF9100U, 0xFF000000U, 0xFF000000U, 0xFF249100U, 0xFFB6B6B6U, 0xFF006D24U, 0xFFB6FF48U, 0xFF6D48DAU, 0xFFFFFF00U, 0xFFDA6D00U, 0xFF004800U, 0xFF006DDAU, 0xFF009100U, 0xFF242424U, 0xFFFFFF6DU, 0xFFFF6DFFU, 0xFF916D00U, 0xFF91FF6DU),
            /* 2C04-0002 */    uintArrayOf(0xFF000000U, 0xFFFFB600U, 0xFF916D00U, 0xFFB6FF48U, 0xFF91FF6DU, 0xFFFF6DFFU, 0xFF009191U, 0xFFB6DAFFU, 0xFFFF0000U, 0xFF9100FFU, 0xFFFFFF6DU, 0xFFFF91FFU, 0xFFFFFFFFU, 0xFFDA6DFFU, 0xFF91DAFFU, 0xFF009100U, 0xFF004800U, 0xFF6DB6FFU, 0xFFB62400U, 0xFFDADADAU, 0xFF00B66DU, 0xFF6DDA00U, 0xFF480000U, 0xFF9191FFU, 0xFF484848U, 0xFFFF00FFU, 0xFF00006DU, 0xFF48FFDAU, 0xFFDAB6FFU, 0xFF6D4800U, 0xFF000000U, 0xFF6D48DAU, 0xFF91006DU, 0xFFFFDA91U, 0xFFFF9100U, 0xFFFFB6FFU, 0xFF006DDAU, 0xFF6D2400U, 0xFFB6B6B6U, 0xFF0000DAU, 0xFFB600FFU, 0xFFFFDA00U, 0xFF6D6D6DU, 0xFF244800U, 0xFF0048FFU, 0xFF000000U, 0xFFDADA00U, 0xFFFFFFFFU, 0xFFDAB66DU, 0xFF242424U, 0xFF00FF00U, 0xFFDA6D00U, 0xFF004848U, 0xFF002491U, 0xFFFF0091U, 0xFF249100U, 0xFF000000U, 0xFF00FFFFU, 0xFF914800U, 0xFFFFFF00U, 0xFFFFB6B6U, 0xFFB6006DU, 0xFF006D24U, 0xFF919191U),
            /* 2C04-0003 */    uintArrayOf(0xFFB600FFU, 0xFFFF6DFFU, 0xFF91FF6DU, 0xFFB6B6B6U, 0xFF009100U, 0xFFFFFFFFU, 0xFFB6DAFFU, 0xFF244800U, 0xFF002491U, 0xFF000000U, 0xFFFFDA91U, 0xFF6D4800U, 0xFFFF0091U, 0xFFDADADAU, 0xFFDAB66DU, 0xFF91DAFFU, 0xFF9191FFU, 0xFF009191U, 0xFFB6006DU, 0xFF0048FFU, 0xFF249100U, 0xFF916D00U, 0xFFDA6D00U, 0xFF00B66DU, 0xFF6D6D6DU, 0xFF6D48DAU, 0xFF000000U, 0xFF0000DAU, 0xFFFF0000U, 0xFFB62400U, 0xFFFF91FFU, 0xFFFFB6B6U, 0xFFDA6DFFU, 0xFF004800U, 0xFF00006DU, 0xFFFFFF00U, 0xFF242424U, 0xFFFFB600U, 0xFFFF9100U, 0xFFFFFFFFU, 0xFF6DDA00U, 0xFF91006DU, 0xFF6DB6FFU, 0xFFFF00FFU, 0xFF006DDAU, 0xFF919191U, 0xFF000000U, 0xFF6D2400U, 0xFF00FFFFU, 0xFF480000U, 0xFFB6FF48U, 0xFFFFB6FFU, 0xFF914800U, 0xFF00FF00U, 0xFFDADA00U, 0xFF484848U, 0xFF006D24U, 0xFF000000U, 0xFFDAB6FFU, 0xFFFFFF6DU, 0xFF9100FFU, 0xFF48FFDAU, 0xFFFFDA00U, 0xFF004848U),
            /* 2C04-0004 */    uintArrayOf(0xFF916D00U, 0xFF6D48DAU, 0xFF009191U, 0xFFDADA00U, 0xFF000000U, 0xFFFFB6B6U, 0xFF002491U, 0xFFDA6D00U, 0xFFB6B6B6U, 0xFF6D2400U, 0xFF00FF00U, 0xFF00006DU, 0xFFFFDA91U, 0xFFFFFF00U, 0xFF009100U, 0xFFB6FF48U, 0xFFFF6DFFU, 0xFF480000U, 0xFF0048FFU, 0xFFFF91FFU, 0xFF000000U, 0xFF484848U, 0xFFB62400U, 0xFFFF9100U, 0xFFDAB66DU, 0xFF00B66DU, 0xFF9191FFU, 0xFF249100U, 0xFF91006DU, 0xFF000000U, 0xFF91FF6DU, 0xFF6DB6FFU, 0xFFB6006DU, 0xFF006D24U, 0xFF914800U, 0xFF0000DAU, 0xFF9100FFU, 0xFFB600FFU, 0xFF6D6D6DU, 0xFFFF0091U, 0xFF004848U, 0xFFDADADAU, 0xFF006DDAU, 0xFF004800U, 0xFF242424U, 0xFFFFFF6DU, 0xFF919191U, 0xFFFF00FFU, 0xFFFFB6FFU, 0xFFFFFFFFU, 0xFF6D4800U, 0xFFFF0000U, 0xFFFFDA00U, 0xFF48FFDAU, 0xFFFFFFFFU, 0xFF91DAFFU, 0xFF000000U, 0xFFFFB600U, 0xFFDA6DFFU, 0xFFB6DAFFU, 0xFF6DDA00U, 0xFFDAB6FFU, 0xFF00FFFFU, 0xFF244800U),
            /* 2C05-01 */      uintArrayOf(0xFF6D6D6DU, 0xFF002491U, 0xFF0000DAU, 0xFF6D48DAU, 0xFF91006DU, 0xFFB6006DU, 0xFFB62400U, 0xFF914800U, 0xFF6D4800U, 0xFF244800U, 0xFF006D24U, 0xFF009100U, 0xFF004848U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFB6B6B6U, 0xFF006DDAU, 0xFF0048FFU, 0xFF9100FFU, 0xFFB600FFU, 0xFFFF0091U, 0xFFFF0000U, 0xFFDA6D00U, 0xFF916D00U, 0xFF249100U, 0xFF009100U, 0xFF00B66DU, 0xFF009191U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFF6DB6FFU, 0xFF9191FFU, 0xFFDA6DFFU, 0xFFFF00FFU, 0xFFFF6DFFU, 0xFFFF9100U, 0xFFFFB600U, 0xFFDADA00U, 0xFF6DDA00U, 0xFF00FF00U, 0xFF48FFDAU, 0xFF00FFFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFFB6DAFFU, 0xFFDAB6FFU, 0xFFFFB6FFU, 0xFFFF91FFU, 0xFFFFB6B6U, 0xFFFFDA91U, 0xFFFFFF48U, 0xFFFFFF6DU, 0xFFB6FF48U, 0xFF91FF6DU, 0xFF48FFDAU, 0xFF91DAFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U),
            /* 2C05-02 */      uintArrayOf(0xFF6D6D6DU, 0xFF002491U, 0xFF0000DAU, 0xFF6D48DAU, 0xFF91006DU, 0xFFB6006DU, 0xFFB62400U, 0xFF914800U, 0xFF6D4800U, 0xFF244800U, 0xFF006D24U, 0xFF009100U, 0xFF004848U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFB6B6B6U, 0xFF006DDAU, 0xFF0048FFU, 0xFF9100FFU, 0xFFB600FFU, 0xFFFF0091U, 0xFFFF0000U, 0xFFDA6D00U, 0xFF916D00U, 0xFF249100U, 0xFF009100U, 0xFF00B66DU, 0xFF009191U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFF6DB6FFU, 0xFF9191FFU, 0xFFDA6DFFU, 0xFFFF00FFU, 0xFFFF6DFFU, 0xFFFF9100U, 0xFFFFB600U, 0xFFDADA00U, 0xFF6DDA00U, 0xFF00FF00U, 0xFF48FFDAU, 0xFF00FFFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFFB6DAFFU, 0xFFDAB6FFU, 0xFFFFB6FFU, 0xFFFF91FFU, 0xFFFFB6B6U, 0xFFFFDA91U, 0xFFFFFF48U, 0xFFFFFF6DU, 0xFFB6FF48U, 0xFF91FF6DU, 0xFF48FFDAU, 0xFF91DAFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U),
            /* 2C05-03 */      uintArrayOf(0xFF6D6D6DU, 0xFF002491U, 0xFF0000DAU, 0xFF6D48DAU, 0xFF91006DU, 0xFFB6006DU, 0xFFB62400U, 0xFF914800U, 0xFF6D4800U, 0xFF244800U, 0xFF006D24U, 0xFF009100U, 0xFF004848U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFB6B6B6U, 0xFF006DDAU, 0xFF0048FFU, 0xFF9100FFU, 0xFFB600FFU, 0xFFFF0091U, 0xFFFF0000U, 0xFFDA6D00U, 0xFF916D00U, 0xFF249100U, 0xFF009100U, 0xFF00B66DU, 0xFF009191U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFF6DB6FFU, 0xFF9191FFU, 0xFFDA6DFFU, 0xFFFF00FFU, 0xFFFF6DFFU, 0xFFFF9100U, 0xFFFFB600U, 0xFFDADA00U, 0xFF6DDA00U, 0xFF00FF00U, 0xFF48FFDAU, 0xFF00FFFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFFB6DAFFU, 0xFFDAB6FFU, 0xFFFFB6FFU, 0xFFFF91FFU, 0xFFFFB6B6U, 0xFFFFDA91U, 0xFFFFFF48U, 0xFFFFFF6DU, 0xFFB6FF48U, 0xFF91FF6DU, 0xFF48FFDAU, 0xFF91DAFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U),
            /* 2C05-04 */      uintArrayOf(0xFF6D6D6DU, 0xFF002491U, 0xFF0000DAU, 0xFF6D48DAU, 0xFF91006DU, 0xFFB6006DU, 0xFFB62400U, 0xFF914800U, 0xFF6D4800U, 0xFF244800U, 0xFF006D24U, 0xFF009100U, 0xFF004848U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFB6B6B6U, 0xFF006DDAU, 0xFF0048FFU, 0xFF9100FFU, 0xFFB600FFU, 0xFFFF0091U, 0xFFFF0000U, 0xFFDA6D00U, 0xFF916D00U, 0xFF249100U, 0xFF009100U, 0xFF00B66DU, 0xFF009191U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFF6DB6FFU, 0xFF9191FFU, 0xFFDA6DFFU, 0xFFFF00FFU, 0xFFFF6DFFU, 0xFFFF9100U, 0xFFFFB600U, 0xFFDADA00U, 0xFF6DDA00U, 0xFF00FF00U, 0xFF48FFDAU, 0xFF00FFFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFFB6DAFFU, 0xFFDAB6FFU, 0xFFFFB6FFU, 0xFFFF91FFU, 0xFFFFB6B6U, 0xFFFFDA91U, 0xFFFFFF48U, 0xFFFFFF6DU, 0xFFB6FF48U, 0xFF91FF6DU, 0xFF48FFDAU, 0xFF91DAFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U),
            /* 2C05-05 */      uintArrayOf(0xFF6D6D6DU, 0xFF002491U, 0xFF0000DAU, 0xFF6D48DAU, 0xFF91006DU, 0xFFB6006DU, 0xFFB62400U, 0xFF914800U, 0xFF6D4800U, 0xFF244800U, 0xFF006D24U, 0xFF009100U, 0xFF004848U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFB6B6B6U, 0xFF006DDAU, 0xFF0048FFU, 0xFF9100FFU, 0xFFB600FFU, 0xFFFF0091U, 0xFFFF0000U, 0xFFDA6D00U, 0xFF916D00U, 0xFF249100U, 0xFF009100U, 0xFF00B66DU, 0xFF009191U, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFF6DB6FFU, 0xFF9191FFU, 0xFFDA6DFFU, 0xFFFF00FFU, 0xFFFF6DFFU, 0xFFFF9100U, 0xFFFFB600U, 0xFFDADA00U, 0xFF6DDA00U, 0xFF00FF00U, 0xFF48FFDAU, 0xFF00FFFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U, 0xFFFFFFFFU, 0xFFB6DAFFU, 0xFFDAB6FFU, 0xFFFFB6FFU, 0xFFFF91FFU, 0xFFFFB6B6U, 0xFFFFDA91U, 0xFFFFFF48U, 0xFFFFFF6DU, 0xFFB6FF48U, 0xFF91FF6DU, 0xFF48FFDAU, 0xFF91DAFFU, 0xFF000000U, 0xFF000000U, 0xFF000000U)
        )

        // @formatter:on

        @JvmStatic val DEFAULT_PALETTE = uintArrayOf(
            0xFF666666U, 0xFF002A88U, 0xFF1412A7U, 0xFF3B00A4U,
            0xFF5C007EU, 0xFF6E0040U, 0xFF6C0600U, 0xFF561D00U,
            0xFF333500U, 0xFF0B4800U, 0xFF005200U, 0xFF004F08U,
            0xFF00404DU, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFADADADU, 0xFF155FD9U, 0xFF4240FFU, 0xFF7527FEU,
            0xFFA01ACCU, 0xFFB71E7BU, 0xFFB53120U, 0xFF994E00U,
            0xFF6B6D00U, 0xFF388700U, 0xFF0C9300U, 0xFF008F32U,
            0xFF007C8DU, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFFFFEFFU, 0xFF64B0FFU, 0xFF9290FFU, 0xFFC676FFU,
            0xFFF36AFFU, 0xFFFE6ECCU, 0xFFFE8170U, 0xFFEA9E22U,
            0xFFBCBE00U, 0xFF88D800U, 0xFF5CE430U, 0xFF45E082U,
            0xFF48CDDEU, 0xFF4F4F4FU, 0xFF000000U, 0xFF000000U,
            0xFFFFFEFFU, 0xFFC0DFFFU, 0xFFD3D2FFU, 0xFFE8C8FFU,
            0xFFFBC2FFU, 0xFFFEC4EAU, 0xFFFECCC5U, 0xFFF7D8A5U,
            0xFFE4E594U, 0xFFCFEF96U, 0xFFBDF4ABU, 0xFFB3F3CCU,
            0xFFB5EBF2U, 0xFFB8B8B8U, 0xFF000000U, 0xFF000000U
        )

        @JvmStatic val UNSATURATED_PALETTE = uintArrayOf(
            0xFF6B6B6BU, 0xFF001E87U, 0xFF1F0B96U, 0xFF3B0C87U,
            0xFF590D61U, 0xFF5E0528U, 0xFF551100U, 0xFF461B00U,
            0xFF303200U, 0xFF0A4800U, 0xFF004E00U, 0xFF004619U,
            0xFF003A58U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFB2B2B2U, 0xFF1A53D1U, 0xFF4835EEU, 0xFF7123ECU,
            0xFF9A1EB7U, 0xFFA51E62U, 0xFFA52D19U, 0xFF874B00U,
            0xFF676900U, 0xFF298400U, 0xFF038B00U, 0xFF008240U,
            0xFF007891U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFF63ADFDU, 0xFF908AFEU, 0xFFB977FCU,
            0xFFE771FEU, 0xFFF76FC9U, 0xFFF5836AU, 0xFFDD9C29U,
            0xFFBDB807U, 0xFF84D107U, 0xFF5BDC3BU, 0xFF48D77DU,
            0xFF48CCCEU, 0xFF555555U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFFC4E3FEU, 0xFFD7D5FEU, 0xFFE6CDFEU,
            0xFFF9CAFEU, 0xFFFEC9F0U, 0xFFFED1C7U, 0xFFF7DCACU,
            0xFFE8E89CU, 0xFFD1F29DU, 0xFFBFF4B1U, 0xFFB7F5CDU,
            0xFFB7F0EEU, 0xFFBEBEBEU, 0xFF000000U, 0xFF000000U
        )

        @JvmStatic val YUV_PALETTE = uintArrayOf(
            0xFF666666U, 0xFF002A88U, 0xFF1412A7U, 0xFF3B00A4U,
            0xFF5C007EU, 0xFF6E0040U, 0xFF6C0700U, 0xFF561D00U,
            0xFF333500U, 0xFF0C4800U, 0xFF005200U, 0xFF004C18U,
            0xFF003E5BU, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFADADADU, 0xFF155FD9U, 0xFF4240FFU, 0xFF7527FEU,
            0xFFA01ACCU, 0xFFB71E7BU, 0xFFB53120U, 0xFF994E00U,
            0xFF6B6D00U, 0xFF388700U, 0xFF0D9300U, 0xFF008C47U,
            0xFF007AA0U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFF64B0FFU, 0xFF9290FFU, 0xFFC676FFU,
            0xFFF26AFFU, 0xFFFF6ECCU, 0xFFFF8170U, 0xFFEA9E22U,
            0xFFBCBE00U, 0xFF88D800U, 0xFF5CE430U, 0xFF45E082U,
            0xFF48CDDEU, 0xFF4F4F4FU, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFFC0DFFFU, 0xFFD3D2FFU, 0xFFE8C8FFU,
            0xFFFAC2FFU, 0xFFFFC4EAU, 0xFFFFCCC5U, 0xFFF7D8A5U,
            0xFFE4E594U, 0xFFCFEF96U, 0xFFBDF4ABU, 0xFFB3F3CCU,
            0xFFB5EBF2U, 0xFFB8B8B8U, 0xFF000000U, 0xFF000000U
        )

        @JvmStatic val NESTOPIA_PALETTE = uintArrayOf(
            0xFF6D6D6DU, 0xFF002492U, 0xFF0000DBU, 0xFF6D49DBU,
            0xFF92006DU, 0xFFB6006DU, 0xFFB62400U, 0xFF924900U,
            0xFF6D4900U, 0xFF244900U, 0xFF006D24U, 0xFF009200U,
            0xFF004949U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFB6B6B6U, 0xFF006DDBU, 0xFF0049FFU, 0xFF9200FFU,
            0xFFB600FFU, 0xFFFF0092U, 0xFFFF0000U, 0xFFDB6D00U,
            0xFF926D00U, 0xFF249200U, 0xFF009200U, 0xFF00B66DU,
            0xFF009292U, 0xFF242424U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFF6DB6FFU, 0xFF9292FFU, 0xFFDB6DFFU,
            0xFFFF00FFU, 0xFFFF6DFFU, 0xFFFF9200U, 0xFFFFB600U,
            0xFFDBDB00U, 0xFF6DDB00U, 0xFF00FF00U, 0xFF49FFDBU,
            0xFF00FFFFU, 0xFF494949U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFFB6DBFFU, 0xFFDBB6FFU, 0xFFFFB6FFU,
            0xFFFF92FFU, 0xFFFFB6B6U, 0xFFFFDB92U, 0xFFFFFF49U,
            0xFFFFFF6DU, 0xFFB6FF49U, 0xFF92FF6DU, 0xFF49FFDBU,
            0xFF92DBFFU, 0xFF929292U, 0xFF000000U, 0xFF000000U
        )

        @JvmStatic val COMPOSITE_DIRECT_PALETTE = uintArrayOf(
            0xFF656565U, 0xFF00127DU, 0xFF18008EU, 0xFF360082U,
            0xFF56005DU, 0xFF5A0018U, 0xFF4F0500U, 0xFF381900U,
            0xFF1D3100U, 0xFF003D00U, 0xFF004100U, 0xFF003B17U,
            0xFF002E55U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFAFAFAFU, 0xFF194EC8U, 0xFF472FE3U, 0xFF6B1FD7U,
            0xFF931BAEU, 0xFF9E1A5EU, 0xFF993200U, 0xFF7B4B00U,
            0xFF5B6700U, 0xFF267A00U, 0xFF008200U, 0xFF007A3EU,
            0xFF006E8AU, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFF64A9FFU, 0xFF8E89FFU, 0xFFB676FFU,
            0xFFE06FFFU, 0xFFEF6CC4U, 0xFFF0806AU, 0xFFD8982CU,
            0xFFB9B40AU, 0xFF83CB0CU, 0xFF5BD63FU, 0xFF4AD17EU,
            0xFF4DC7CBU, 0xFF4C4C4CU, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFFC7E5FFU, 0xFFD9D9FFU, 0xFFE9D1FFU,
            0xFFF9CEFFU, 0xFFFFCCF1U, 0xFFFFD4CBU, 0xFFF8DFB1U,
            0xFFEDEAA4U, 0xFFD6F4A4U, 0xFFC5F8B8U, 0xFFBEF6D3U,
            0xFFBFF1F1U, 0xFFB9B9B9U, 0xFF000000U, 0xFF000000U
        )

        @JvmStatic val NES_CLASSIC_PALETTE = uintArrayOf(
            0xFF60615FU, 0xFF000083U, 0xFF1D0195U, 0xFF340875U,
            0xFF51055EU, 0xFF56000FU, 0xFF4C0700U, 0xFF372308U,
            0xFF203A0BU, 0xFF0F4B0EU, 0xFF194C16U, 0xFF02421EU,
            0xFF023154U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFA9AAA8U, 0xFF104BBFU, 0xFF4712D8U, 0xFF6300CAU,
            0xFF8800A9U, 0xFF930B46U, 0xFF8A2D04U, 0xFF6F5206U,
            0xFF5C7114U, 0xFF1B8D12U, 0xFF199509U, 0xFF178448U,
            0xFF206B8EU, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFFBFBFBU, 0xFF6699F8U, 0xFF8974F9U, 0xFFAB58F8U,
            0xFFD557EFU, 0xFFDE5FA9U, 0xFFDC7F59U, 0xFFC7A224U,
            0xFFA7BE03U, 0xFF75D703U, 0xFF60E34FU, 0xFF3CD68DU,
            0xFF56C9CCU, 0xFF414240U, 0xFF000000U, 0xFF000000U,
            0xFFFBFBFBU, 0xFFBED4FAU, 0xFFC9C7F9U, 0xFFD7BEFAU,
            0xFFE8B8F9U, 0xFFF5BAE5U, 0xFFF3CAC2U, 0xFFDFCDA7U,
            0xFFD9E09CU, 0xFFC9EB9EU, 0xFFC0EDB8U, 0xFFB5F4C7U,
            0xFFB9EAE9U, 0xFFABABABU, 0xFF000000U, 0xFF000000U
        )

        @JvmStatic val ORIGINAL_HARDWARE_PALETTE = uintArrayOf(
            0xFF6A6D6AU, 0xFF00127DU, 0xFF1E008AU, 0xFF3B007DU,
            0xFF56005DU, 0xFF5A0018U, 0xFF4F0D00U, 0xFF381E00U,
            0xFF203100U, 0xFF003D00U, 0xFF004000U, 0xFF003B1EU,
            0xFF002E55U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFB9BCB9U, 0xFF194EC8U, 0xFF472FE3U, 0xFF751FD7U,
            0xFF931EADU, 0xFF9E245EU, 0xFF963800U, 0xFF7B5000U,
            0xFF5B6700U, 0xFF267A00U, 0xFF007F00U, 0xFF007842U,
            0xFF006E8AU, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFF69AEFFU, 0xFF9798FFU, 0xFFB687FFU,
            0xFFE278FFU, 0xFFF279C7U, 0xFFF58F6FU, 0xFFDDA932U,
            0xFFBCB70DU, 0xFF88D015U, 0xFF60DB49U, 0xFF4FD687U,
            0xFF50CACEU, 0xFF515451U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFFCCEAFFU, 0xFFDEE2FFU, 0xFFEEDAFFU,
            0xFFFAD7FDU, 0xFFFDD7F6U, 0xFFFDDCD0U, 0xFFFAE8B6U,
            0xFFF2F1A9U, 0xFFDBFBA9U, 0xFFCAFFBDU, 0xFFC3FBD8U,
            0xFFC4F6F6U, 0xFFBEC1BEU, 0xFF000000U, 0xFF000000U
        )

        @JvmStatic val PVM_STYLE_PALETTE = uintArrayOf(
            0xFF696964U, 0xFF001774U, 0xFF28007DU, 0xFF3E006DU,
            0xFF560057U, 0xFF5E0013U, 0xFF531A00U, 0xFF3B2400U,
            0xFF2A3000U, 0xFF143A00U, 0xFF003F00U, 0xFF003B1EU,
            0xFF003050U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFB9B9B4U, 0xFF1453B9U, 0xFF4D2CDAU, 0xFF7A1EC8U,
            0xFF98189CU, 0xFF9D2344U, 0xFFA03E00U, 0xFF8D5500U,
            0xFF656D00U, 0xFF2C7900U, 0xFF008100U, 0xFF007D42U,
            0xFF00788AU, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFF69A8FFU, 0xFF9A96FFU, 0xFFC28AFAU,
            0xFFEA7DFAU, 0xFFF387B4U, 0xFFF1986CU, 0xFFE6B327U,
            0xFFD7C805U, 0xFF90DF07U, 0xFF64E53CU, 0xFF45E27DU,
            0xFF48D5D9U, 0xFF4B4B46U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFFD2EAFFU, 0xFFE2E2FFU, 0xFFF2D8FFU,
            0xFFF8D2FFU, 0xFFF8D9EAU, 0xFFFADEB9U, 0xFFF9E89BU,
            0xFFF3F28CU, 0xFFD3FA91U, 0xFFB8FCA8U, 0xFFAEFACAU,
            0xFFCAF3F3U, 0xFFBEBEB9U, 0xFF000000U, 0xFF000000U
        )

        @JvmStatic val SONY_CXA_2025_PALETTE = uintArrayOf(
            0xFF585858U, 0xFF00238CU, 0xFF00139BU, 0xFF2D0585U,
            0xFF5D0052U, 0xFF7A0017U, 0xFF7A0800U, 0xFF5F1800U,
            0xFF352A00U, 0xFF093900U, 0xFF003F00U, 0xFF003C22U,
            0xFF00325DU, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFA1A1A1U, 0xFF0053EEU, 0xFF153CFEU, 0xFF6028E4U,
            0xFFA91D98U, 0xFFD41E41U, 0xFFD22C00U, 0xFFAA4400U,
            0xFF6C5E00U, 0xFF2D7300U, 0xFF007D06U, 0xFF007852U,
            0xFF0069A9U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFF1FA5FEU, 0xFF5E89FEU, 0xFFB572FEU,
            0xFFFE65F6U, 0xFFFE6790U, 0xFFFE773CU, 0xFFFE9308U,
            0xFFC4B200U, 0xFF79CA10U, 0xFF3AD54AU, 0xFF11D1A4U,
            0xFF06BFFEU, 0xFF424242U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFFA0D9FEU, 0xFFBDCCFEU, 0xFFE1C2FEU,
            0xFFFEBCFBU, 0xFFFEBDD0U, 0xFFFEC5A9U, 0xFFFED18EU,
            0xFFE9DE86U, 0xFFC7E992U, 0xFFA8EEB0U, 0xFF95ECD9U,
            0xFF91E4FEU, 0xFFACACACU, 0xFF000000U, 0xFF000000U
        )

        @JvmStatic val WAVEBEAM_PALETTE = uintArrayOf(
            0xFF6B6B6BU, 0xFF001B88U, 0xFF21009AU, 0xFF40008CU,
            0xFF600067U, 0xFF64001EU, 0xFF590800U, 0xFF481600U,
            0xFF283600U, 0xFF004500U, 0xFF004908U, 0xFF00421DU,
            0xFF003659U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFB4B4B4U, 0xFF1555D3U, 0xFF4337EFU, 0xFF7425DFU,
            0xFF9C19B9U, 0xFFAC0F64U, 0xFFAA2C00U, 0xFF8A4B00U,
            0xFF666B00U, 0xFF218300U, 0xFF008A00U, 0xFF008144U,
            0xFF007691U, 0xFF000000U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFF63B2FFU, 0xFF7C9CFFU, 0xFFC07DFEU,
            0xFFE977FFU, 0xFFF572CDU, 0xFFF4886BU, 0xFFDDA029U,
            0xFFBDBD0AU, 0xFF89D20EU, 0xFF5CDE3EU, 0xFF4BD886U,
            0xFF4DCFD2U, 0xFF525252U, 0xFF000000U, 0xFF000000U,
            0xFFFFFFFFU, 0xFFBCDFFFU, 0xFFD2D2FFU, 0xFFE1C8FFU,
            0xFFEFC7FFU, 0xFFFFC3E1U, 0xFFFFCAC6U, 0xFFF2DAADU,
            0xFFEBE3A0U, 0xFFD2EDA2U, 0xFFBCF4B4U, 0xFFB5F1CEU,
            0xFFB6ECF1U, 0xFFBFBFBFU, 0xFF000000U, 0xFF000000U
        )
    }
}
