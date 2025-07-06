package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControlDevice.Companion.PORT_COUNT
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.EmulationFlag.*
import br.tiagohm.nestalgia.core.GameInputType.*
import br.tiagohm.nestalgia.core.GameInputType.BANDAI_HYPER_SHOT
import br.tiagohm.nestalgia.core.GameInputType.BARCODE_BATTLER
import br.tiagohm.nestalgia.core.GameInputType.BATTLE_BOX
import br.tiagohm.nestalgia.core.GameInputType.EXCITING_BOXING
import br.tiagohm.nestalgia.core.GameInputType.FAMILY_BASIC_KEYBOARD
import br.tiagohm.nestalgia.core.GameInputType.FOUR_PLAYER_ADAPTER
import br.tiagohm.nestalgia.core.GameInputType.FOUR_SCORE
import br.tiagohm.nestalgia.core.GameInputType.JISSEN_MAHJONG
import br.tiagohm.nestalgia.core.GameInputType.KONAMI_HYPER_SHOT
import br.tiagohm.nestalgia.core.GameInputType.OEKA_KIDS_TABLET
import br.tiagohm.nestalgia.core.GameInputType.PARTY_TAP
import br.tiagohm.nestalgia.core.GameInputType.POWER_PAD_SIDE_A
import br.tiagohm.nestalgia.core.GameInputType.POWER_PAD_SIDE_B
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

@Suppress("DuplicatedCode")
class EmulationSettings : Snapshotable, Resetable {

    @PublishedApi @JvmField internal val flags = BooleanArray(128)

    // Standard Controller.
    @JvmField val standardControllerTurboSpeed = AtomicInteger(2)

    // Bandai Hyper Shot.
    @JvmField val bandaiHyperShotTurboSpeed = AtomicInteger(2)

    // Zapper.
    @JvmField val zapperDetectionRadius = IntArray(PORT_COUNT) { 1 }

    // Arkanoid.
    @JvmField val arkanoidSensibility = IntArray(PORT_COUNT) { 0 }

    // Console.
    @JvmField var region = Region.AUTO

    @JvmField var ramPowerOnState = RamPowerOnState.ALL_ZEROS

    @JvmField var dipSwitches = 0

    @JvmField val port1 = ControllerSettings()
    @JvmField val port2 = ControllerSettings()
    @JvmField val expansionPort = ControllerSettings()
    @JvmField val subPort1 = Array(4) { ControllerSettings() }
    @JvmField val expansionSubPort = Array(4) { ControllerSettings() }
    @JvmField val mapperPort = ControllerSettings()
    @Volatile private var needControllerUpdate = false

    @JvmField var consoleType = ConsoleType.NES_001

    // CPU
    @Volatile private var emulationSpeed = 100
    @Volatile private var turboSpeed = 300
    @Volatile private var rewindSpeed = 100

    // APU
    @Volatile private var needAudioSettingsUpdate = false
    @Volatile var enableDmcSampleDuplicationGlitch = false

    var sampleRate = 48000
        set(value) {
            if (value != field) {
                field = value
                needAudioSettingsUpdate = true
            }
        }

    // PPU
    @JvmField var inputPollScanline = 241
    @JvmField var disableOverclocking = false

    @JvmField val palette = IntArray(512)

    var extraScanlinesBeforeNmi = 0
        get() = if (disableOverclocking) 0 else field

    var extraScanlinesAfterNmi = 0
        get() = if (disableOverclocking) 0 else field

    var backgroundEnabled = true
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
            field = value
            updatePalette(value)
        }

    private fun resetFlags() {
        flags.fill(false)
        flag(AUTO_CONFIGURE_INPUT, true)
        flag(FDS_AUTO_LOAD_DISK, true)
    }

    override fun reset(softReset: Boolean) {
        restoreState(Snapshot(0))
    }

    fun markAsNeedControllerUpdate() {
        needControllerUpdate = true
    }

    override fun saveState(s: Snapshot) {
        s.write("flags", flags.clone())
        s.write("standardControllerTurboSpeed", standardControllerTurboSpeed.get())
        s.write("bandaiHyperShotTurboSpeed", bandaiHyperShotTurboSpeed.get())
        s.write("zapperDetectionRadius", zapperDetectionRadius)
        s.write("region", region)
        s.write("ramPowerOnState", ramPowerOnState)
        s.write("dipSwitches", dipSwitches)
        s.write("port1", port1)
        s.write("port2", port2)
        s.write("expansionPort", expansionPort)
        repeat(subPort1.size) { s.write("subPort1$it", subPort1[it]) }
        repeat(expansionSubPort.size) { s.write("expansionSubPort$it", expansionSubPort[it]) }
        s.write("mapperPort", mapperPort)
        s.write("needControllerUpdate", needControllerUpdate)
        s.write("consoleType", consoleType)
        s.write("emulationSpeed", emulationSpeed)
        s.write("turboSpeed", turboSpeed)
        s.write("rewindSpeed", rewindSpeed)
        s.write("needAudioSettingsUpdate", needAudioSettingsUpdate)
        s.write("sampleRate", sampleRate)
        s.write("inputPollScanline", inputPollScanline)
        s.write("disableOverclocking", disableOverclocking)
        s.write("extraScanlinesBeforeNmi", extraScanlinesBeforeNmi)
        s.write("extraScanlinesAfterNmi", extraScanlinesAfterNmi)
        s.write("ppuModel", ppuModel)
        s.write("paletteType", paletteType)
    }

    override fun restoreState(s: Snapshot) {
        needControllerUpdate = s.readBoolean("needControllerUpdate")
        needAudioSettingsUpdate = s.readBoolean("needAudioSettingsUpdate")
        s.readBooleanArray("flags", flags) ?: resetFlags()
        standardControllerTurboSpeed.set(s.readInt("standardControllerTurboSpeed", 2))
        bandaiHyperShotTurboSpeed.set(s.readInt("bandaiHyperShotTurboSpeed", 2))
        s.readIntArrayOrFill("zapperDetectionRadius", zapperDetectionRadius, 1)
        region = s.readEnum("region", Region.AUTO)
        ramPowerOnState = s.readEnum("ramPowerOnState", RamPowerOnState.ALL_ZEROS)
        dipSwitches = s.readInt("dipSwitches")
        s.readSnapshotable("port1", port1)
        s.readSnapshotable("port2", port2)
        s.readSnapshotable("expansionPort", expansionPort)
        repeat(subPort1.size) { s.readSnapshotable("subPort1$it", subPort1[it]) }
        repeat(expansionSubPort.size) { s.readSnapshotable("expansionSubPort$it", expansionSubPort[it]) }
        s.readSnapshotable("mapperPort", mapperPort)
        consoleType = s.readEnum("consoleType", ConsoleType.NES_001)
        emulationSpeed = s.readInt("emulationSpeed", 100)
        turboSpeed = s.readInt("turboSpeed", 300)
        rewindSpeed = s.readInt("rewindSpeed", 100)
        sampleRate = s.readInt("sampleRate", 48000)
        inputPollScanline = s.readInt("inputPollScanline", 241)
        disableOverclocking = s.readBoolean("disableOverclocking")
        extraScanlinesBeforeNmi = s.readInt("extraScanlinesBeforeNmi")
        extraScanlinesAfterNmi = s.readInt("extraScanlinesAfterNmi")
        ppuModel = s.readEnum("ppuModel", PpuModel.PPU_2C02)
        paletteType = s.readEnum("paletteType", PaletteType.DEFAULT)
        backgroundEnabled = !flag(DISABLE_BACKGROUND)
        spritesEnabled = !flag(DISABLE_SPRITES)
    }

    fun needControllerUpdate(): Boolean {
        return if (needControllerUpdate) {
            LOG.info("controller was updated")
            needControllerUpdate = false
            true
        } else {
            false
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun flag(flag: EmulationFlag): Boolean {
        return flags[flag.ordinal]
    }

    fun flag(flag: EmulationFlag, value: Boolean) {
        flags[flag.ordinal] = value

        when (flag) {
            DISABLE_BACKGROUND -> backgroundEnabled = !value
            DISABLE_SPRITES -> spritesEnabled = !value
            USE_CUSTOM_VS_PALETTE -> updateCurrentPalette()
            else -> Unit
        }
    }

    fun emulationSpeed(ignoreTurbo: Boolean = true) = when {
        ignoreTurbo -> emulationSpeed
        flag(FORCE_MAX_SPEED) -> 0
        flag(TURBO) -> turboSpeed
        flag(REWIND) -> rewindSpeed
        else -> emulationSpeed
    }

    fun emulationSpeed(speed: Int) {
        if (speed != emulationSpeed) {
            emulationSpeed = speed
            needAudioSettingsUpdate = true
        }
    }

    fun updateCurrentPalette() {
        updatePalette(paletteType)
    }

    private fun updatePalette(palette: Palette) {
        require(palette.size == 64 || palette.size == 512) { "Invalid palette buffer size" }

        val data = IntArray(512)
        palette.data.copyInto(data)

        if (!palette.isFullColor) {
            generateFullColorPalette(data)
        }

        updateCurrentPalette(data)
    }

    private fun updateCurrentPalette(palette: IntArray) {
        when {
            flag(USE_CUSTOM_VS_PALETTE) -> {
                for (i in 0 until 64) {
                    for (j in 0 until 8) {
                        this.palette[(j shl 6) or i] = palette[(j shl 6) or PALETTE_LUT[ppuModel.ordinal][i]]
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

    private fun generateFullColorPalette(palette: IntArray) {
        repeat(64) {
            for (j in 0 until 8) {
                var red = (palette[it] shr 16 and 0xFF).toDouble()
                var green = (palette[it] shr 8 and 0xFF).toDouble()
                var blue = (palette[it] and 0xFF).toDouble()

                if (j.bit0) {
                    red *= 1.1
                    green *= 0.9
                    blue *= 0.9
                }

                if (j.bit1) {
                    red *= 0.9
                    green *= 1.1
                    blue *= 0.9
                }

                if (j.bit2) {
                    red *= 0.9
                    green *= 0.9
                    blue *= 1.1
                }

                val r = min(255, red.toInt())
                val g = min(255, green.toInt())
                val b = min(255, blue.toInt())
                val color = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
                palette[(j shl 6) or it] = color
            }
        }
    }

    fun initializeInputDevices(inputType: GameInputType, gameSystem: GameSystem) {
        val controllers = arrayOf(NES_CONTROLLER, NES_CONTROLLER, NONE)

        if (inputType == VS_ZAPPER) {
            // VS Duck Hunt, etc. need the zapper in the first port.
            LOG.info("VS Zapper connected")
            controllers[0] = NES_ZAPPER
        } else if (inputType == ZAPPER) {
            LOG.info("Zapper connected")
            if (gameSystem.isFamicom) {
                controllers[2] = FAMICOM_ZAPPER
            } else {
                controllers[1] = NES_ZAPPER
            }
        } else if (inputType == FOUR_SCORE) {
            LOG.info("Four score connected")
            controllers[0] = ControllerType.FOUR_SCORE
            controllers[1] = NONE
        } else if (inputType == FOUR_PLAYER_ADAPTER) {
            LOG.info("Four player adapter connected")
            controllers[2] = TWO_PLAYER_ADAPTER
        } else if (inputType == ARKANOID_CONTROLLER_FAMICOM) {
            LOG.info("Arkanoid controller (Famicom) connected")
            controllers[2] = FAMICOM_ARKANOID_CONTROLLER
        } else if (inputType == ARKANOID_CONTROLLER_NES) {
            LOG.info("Arkanoid controller (NES) connected")
            controllers[1] = NES_ARKANOID_CONTROLLER
        } else if (inputType == DOUBLE_ARKANOID_CONTROLLER) {
            LOG.info("2 arkanoid controllers (NES) connected")
            controllers[0] = NES_ARKANOID_CONTROLLER
            controllers[1] = NES_ARKANOID_CONTROLLER
        } else if (inputType == OEKA_KIDS_TABLET) {
            LOG.info("Oeka Kids Tablet connected")
            controllers[2] = ControllerType.OEKA_KIDS_TABLET
        } else if (inputType == KONAMI_HYPER_SHOT) {
            LOG.info("Konami Hyper Shot connected")
            controllers[2] = ControllerType.KONAMI_HYPER_SHOT
        } else if (inputType == FAMILY_BASIC_KEYBOARD) {
            LOG.info("Family Basic Keyboard connected")
            controllers[2] = ControllerType.FAMILY_BASIC_KEYBOARD
        } else if (inputType == PARTY_TAP) {
            LOG.info("Party Tap connected")
            controllers[2] = ControllerType.PARTY_TAP
        } else if (inputType == PACHINKO_CONTROLLER) {
            LOG.info("Pachinko controller connected")
            controllers[2] = PACHINKO
        } else if (inputType == EXCITING_BOXING) {
            LOG.info("Exciting Boxing controller connected")
            controllers[2] = ControllerType.EXCITING_BOXING
        } else if (inputType == SUBOR_KEYBOARD_MOUSE_1) {
            LOG.info("Subor mouse connected")
            LOG.info("Subor keyboard connected")
            controllers[2] = ControllerType.SUBOR_KEYBOARD
            controllers[1] = SUBOR_MOUSE
        } else if (inputType == JISSEN_MAHJONG) {
            LOG.info("Jissen Mahjong controller connected")
            controllers[2] = ControllerType.JISSEN_MAHJONG
        } else if (inputType == BARCODE_BATTLER) {
            LOG.info("Barcode Battler barcode reader connected")
            controllers[2] = ControllerType.BARCODE_BATTLER
        } else if (inputType == BANDAI_HYPER_SHOT) {
            LOG.info("Bandai Hyper Shot gun connected")
            controllers[2] = ControllerType.BANDAI_HYPER_SHOT
        } else if (inputType == BATTLE_BOX) {
            LOG.info("Battle Box connected")
            controllers[2] = ControllerType.BATTLE_BOX
        } else if (inputType == TURBO_FILE) {
            LOG.info("Ascii Turbo File connected")
            controllers[2] = ASCII_TURBO_FILE
        } else if (inputType == FAMILY_TRAINER_SIDE_A) {
            LOG.info("Family Trainer mat connected (Side A)")
            controllers[2] = FAMILY_TRAINER_MAT_SIDE_A
        } else if (inputType == FAMILY_TRAINER_SIDE_B) {
            LOG.info("Family Trainer mat connected (Side B)")
            controllers[2] = FAMILY_TRAINER_MAT_SIDE_B
        } else if (inputType == POWER_PAD_SIDE_A) {
            LOG.info("Power Pad connected (Side A)")
            controllers[1] = ControllerType.POWER_PAD_SIDE_A
        } else if (inputType == POWER_PAD_SIDE_B) {
            LOG.info("Power Pad connected (Side B)")
            controllers[1] = ControllerType.POWER_PAD_SIDE_B
        } else {
            LOG.info("2 NES controllers connected")
        }

        port1.type = controllers[0]
        port2.type = controllers[1]
        expansionPort.type = controllers[2]

        if (controllers[0] == ControllerType.FOUR_SCORE) {
            subPort1[0].type = NES_CONTROLLER
            subPort1[1].type = NES_CONTROLLER
            subPort1[2].type = NONE
            subPort1[3].type = NONE
        } else if (controllers[2] == TWO_PLAYER_ADAPTER) {
            expansionSubPort[0].type = NES_CONTROLLER
            expansionSubPort[1].type = NES_CONTROLLER
        }

        markAsNeedControllerUpdate()
    }

    val needsPause
        get() = flag(PAUSED)

    val isInputEnabled
        get() = !flag(IN_BACKGROUND) || flag(ALLOW_BACKGROUND_INPUT)

    fun needAudioSettingsUpdate(): Boolean {
        return if (needAudioSettingsUpdate) {
            LOG.info("audio settings was updated")
            needAudioSettingsUpdate = false
            true
        } else {
            false
        }
    }

    fun populateWithDefault(): Boolean {
        var modified = false
        modified = port1.populateKeyMappingWithDefault() || modified
        modified = port2.populateKeyMappingWithDefault() || modified
        modified = expansionPort.populateKeyMappingWithDefault() || modified

        if (modified) {
            needControllerUpdate()
        }

        return modified
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(EmulationSettings::class.java)

        // @formatter:off
        private val PALETTE_LUT = arrayOf(
            /* 2C02 */      intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63),
            /* 2C03 */      intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 15, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 15, 62, 63),
            /* 2C04-0001 */ intArrayOf(53, 35, 22, 34, 28, 9, 29, 21, 32, 0, 39, 5, 4, 40, 8, 32, 33, 62, 31, 41, 60, 50, 54, 18, 63, 43, 46, 30, 61, 45, 36, 1, 14, 49, 51, 42, 44, 12, 27, 20, 46, 7, 52, 6, 19, 2, 38, 46, 46, 25, 16, 10, 57, 3, 55, 23, 15, 17, 11, 13, 56, 37, 24, 58),
            /* 2C04-0002 */ intArrayOf(46, 39, 24, 57, 58, 37, 28, 49, 22, 19, 56, 52, 32, 35, 60, 11, 15, 33, 6, 61, 27, 41, 30, 34, 29, 36, 14, 43, 50, 8, 46, 3, 4, 54, 38, 51, 17, 31, 16, 2, 20, 63, 0, 9, 18, 46, 40, 32, 62, 13, 42, 23, 12, 1, 21, 25, 46, 44, 7, 55, 53, 5, 10, 45),
            /* 2C04-0003 */ intArrayOf(20, 37, 58, 16, 11, 32, 49, 9, 1, 46, 54, 8, 21, 61, 62, 60, 34, 28, 5, 18, 25, 24, 23, 27, 0, 3, 46, 2, 22, 6, 52, 53, 35, 15, 14, 55, 13, 39, 38, 32, 41, 4, 33, 36, 17, 45, 46, 31, 44, 30, 57, 51, 7, 42, 40, 29, 10, 46, 50, 56, 19, 43, 63, 12),
            /* 2C04-0004 */ intArrayOf(24, 3, 28, 40, 46, 53, 1, 23, 16, 31, 42, 14, 54, 55, 11, 57, 37, 30, 18, 52, 46, 29, 6, 38, 62, 27, 34, 25, 4, 46, 58, 33, 5, 10, 7, 2, 19, 20, 0, 21, 12, 61, 17, 15, 13, 56, 45, 36, 51, 32, 8, 22, 63, 43, 32, 60, 46, 39, 35, 49, 41, 50, 44, 9),
            /* 2C05-01 */   intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 15, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 15, 62, 63),
            /* 2C05-02 */   intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 15, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 15, 62, 63),
            /* 2C05-03 */   intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 15, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 15, 62, 63),
            /* 2C05-04 */   intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 15, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 15, 62, 63),
            /* 2C05-05 */   intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 15, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 15, 62, 63),
        )

        private val PPU_PALETTE_ARGB = arrayOf(
            /* 2C02 */         intArrayOf(0xFF666666.toInt(), 0xFF002A88.toInt(), 0xFF1412A7.toInt(), 0xFF3B00A4.toInt(), 0xFF5C007E.toInt(), 0xFF6E0040.toInt(), 0xFF6C0600.toInt(), 0xFF561D00.toInt(), 0xFF333500.toInt(), 0xFF0B4800.toInt(), 0xFF005200.toInt(), 0xFF004F08.toInt(), 0xFF00404D.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFADADAD.toInt(), 0xFF155FD9.toInt(), 0xFF4240FF.toInt(), 0xFF7527FE.toInt(), 0xFFA01ACC.toInt(), 0xFFB71E7B.toInt(), 0xFFB53120.toInt(), 0xFF994E00.toInt(), 0xFF6B6D00.toInt(), 0xFF388700.toInt(), 0xFF0C9300.toInt(), 0xFF008F32.toInt(), 0xFF007C8D.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFEFF.toInt(), 0xFF64B0FF.toInt(), 0xFF9290FF.toInt(), 0xFFC676FF.toInt(), 0xFFF36AFF.toInt(), 0xFFFE6ECC.toInt(), 0xFFFE8170.toInt(), 0xFFEA9E22.toInt(), 0xFFBCBE00.toInt(), 0xFF88D800.toInt(), 0xFF5CE430.toInt(), 0xFF45E082.toInt(), 0xFF48CDDE.toInt(), 0xFF4F4F4F.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFEFF.toInt(), 0xFFC0DFFF.toInt(), 0xFFD3D2FF.toInt(), 0xFFE8C8FF.toInt(), 0xFFFBC2FF.toInt(), 0xFFFEC4EA.toInt(), 0xFFFECCC5.toInt(), 0xFFF7D8A5.toInt(), 0xFFE4E594.toInt(), 0xFFCFEF96.toInt(), 0xFFBDF4AB.toInt(), 0xFFB3F3CC.toInt(), 0xFFB5EBF2.toInt(), 0xFFB8B8B8.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            /* 2C03 */         intArrayOf(0xFF6D6D6D.toInt(), 0xFF002491.toInt(), 0xFF0000DA.toInt(), 0xFF6D48DA.toInt(), 0xFF91006D.toInt(), 0xFFB6006D.toInt(), 0xFFB62400.toInt(), 0xFF914800.toInt(), 0xFF6D4800.toInt(), 0xFF244800.toInt(), 0xFF006D24.toInt(), 0xFF009100.toInt(), 0xFF004848.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFB6B6B6.toInt(), 0xFF006DDA.toInt(), 0xFF0048FF.toInt(), 0xFF9100FF.toInt(), 0xFFB600FF.toInt(), 0xFFFF0091.toInt(), 0xFFFF0000.toInt(), 0xFFDA6D00.toInt(), 0xFF916D00.toInt(), 0xFF249100.toInt(), 0xFF009100.toInt(), 0xFF00B66D.toInt(), 0xFF009191.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF6DB6FF.toInt(), 0xFF9191FF.toInt(), 0xFFDA6DFF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF6DFF.toInt(), 0xFFFF9100.toInt(), 0xFFFFB600.toInt(), 0xFFDADA00.toInt(), 0xFF6DDA00.toInt(), 0xFF00FF00.toInt(), 0xFF48FFDA.toInt(), 0xFF00FFFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFB6DAFF.toInt(), 0xFFDAB6FF.toInt(), 0xFFFFB6FF.toInt(), 0xFFFF91FF.toInt(), 0xFFFFB6B6.toInt(), 0xFFFFDA91.toInt(), 0xFFFFFF48.toInt(), 0xFFFFFF6D.toInt(), 0xFFB6FF48.toInt(), 0xFF91FF6D.toInt(), 0xFF48FFDA.toInt(), 0xFF91DAFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            /* 2C04-0001 */    intArrayOf(0xFFFFB6B6.toInt(), 0xFFDA6DFF.toInt(), 0xFFFF0000.toInt(), 0xFF9191FF.toInt(), 0xFF009191.toInt(), 0xFF244800.toInt(), 0xFF484848.toInt(), 0xFFFF0091.toInt(), 0xFFFFFFFF.toInt(), 0xFF6D6D6D.toInt(), 0xFFFFB600.toInt(), 0xFFB6006D.toInt(), 0xFF91006D.toInt(), 0xFFDADA00.toInt(), 0xFF6D4800.toInt(), 0xFFFFFFFF.toInt(), 0xFF6DB6FF.toInt(), 0xFFDAB66D.toInt(), 0xFF6D2400.toInt(), 0xFF6DDA00.toInt(), 0xFF91DAFF.toInt(), 0xFFDAB6FF.toInt(), 0xFFFFDA91.toInt(), 0xFF0048FF.toInt(), 0xFFFFDA00.toInt(), 0xFF48FFDA.toInt(), 0xFF000000.toInt(), 0xFF480000.toInt(), 0xFFDADADA.toInt(), 0xFF919191.toInt(), 0xFFFF00FF.toInt(), 0xFF002491.toInt(), 0xFF00006D.toInt(), 0xFFB6DAFF.toInt(), 0xFFFFB6FF.toInt(), 0xFF00FF00.toInt(), 0xFF00FFFF.toInt(), 0xFF004848.toInt(), 0xFF00B66D.toInt(), 0xFFB600FF.toInt(), 0xFF000000.toInt(), 0xFF914800.toInt(), 0xFFFF91FF.toInt(), 0xFFB62400.toInt(), 0xFF9100FF.toInt(), 0xFF0000DA.toInt(), 0xFFFF9100.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF249100.toInt(), 0xFFB6B6B6.toInt(), 0xFF006D24.toInt(), 0xFFB6FF48.toInt(), 0xFF6D48DA.toInt(), 0xFFFFFF00.toInt(), 0xFFDA6D00.toInt(), 0xFF004800.toInt(), 0xFF006DDA.toInt(), 0xFF009100.toInt(), 0xFF242424.toInt(), 0xFFFFFF6D.toInt(), 0xFFFF6DFF.toInt(), 0xFF916D00.toInt(), 0xFF91FF6D.toInt()),
            /* 2C04-0002 */    intArrayOf(0xFF000000.toInt(), 0xFFFFB600.toInt(), 0xFF916D00.toInt(), 0xFFB6FF48.toInt(), 0xFF91FF6D.toInt(), 0xFFFF6DFF.toInt(), 0xFF009191.toInt(), 0xFFB6DAFF.toInt(), 0xFFFF0000.toInt(), 0xFF9100FF.toInt(), 0xFFFFFF6D.toInt(), 0xFFFF91FF.toInt(), 0xFFFFFFFF.toInt(), 0xFFDA6DFF.toInt(), 0xFF91DAFF.toInt(), 0xFF009100.toInt(), 0xFF004800.toInt(), 0xFF6DB6FF.toInt(), 0xFFB62400.toInt(), 0xFFDADADA.toInt(), 0xFF00B66D.toInt(), 0xFF6DDA00.toInt(), 0xFF480000.toInt(), 0xFF9191FF.toInt(), 0xFF484848.toInt(), 0xFFFF00FF.toInt(), 0xFF00006D.toInt(), 0xFF48FFDA.toInt(), 0xFFDAB6FF.toInt(), 0xFF6D4800.toInt(), 0xFF000000.toInt(), 0xFF6D48DA.toInt(), 0xFF91006D.toInt(), 0xFFFFDA91.toInt(), 0xFFFF9100.toInt(), 0xFFFFB6FF.toInt(), 0xFF006DDA.toInt(), 0xFF6D2400.toInt(), 0xFFB6B6B6.toInt(), 0xFF0000DA.toInt(), 0xFFB600FF.toInt(), 0xFFFFDA00.toInt(), 0xFF6D6D6D.toInt(), 0xFF244800.toInt(), 0xFF0048FF.toInt(), 0xFF000000.toInt(), 0xFFDADA00.toInt(), 0xFFFFFFFF.toInt(), 0xFFDAB66D.toInt(), 0xFF242424.toInt(), 0xFF00FF00.toInt(), 0xFFDA6D00.toInt(), 0xFF004848.toInt(), 0xFF002491.toInt(), 0xFFFF0091.toInt(), 0xFF249100.toInt(), 0xFF000000.toInt(), 0xFF00FFFF.toInt(), 0xFF914800.toInt(), 0xFFFFFF00.toInt(), 0xFFFFB6B6.toInt(), 0xFFB6006D.toInt(), 0xFF006D24.toInt(), 0xFF919191.toInt()),
            /* 2C04-0003 */    intArrayOf(0xFFB600FF.toInt(), 0xFFFF6DFF.toInt(), 0xFF91FF6D.toInt(), 0xFFB6B6B6.toInt(), 0xFF009100.toInt(), 0xFFFFFFFF.toInt(), 0xFFB6DAFF.toInt(), 0xFF244800.toInt(), 0xFF002491.toInt(), 0xFF000000.toInt(), 0xFFFFDA91.toInt(), 0xFF6D4800.toInt(), 0xFFFF0091.toInt(), 0xFFDADADA.toInt(), 0xFFDAB66D.toInt(), 0xFF91DAFF.toInt(), 0xFF9191FF.toInt(), 0xFF009191.toInt(), 0xFFB6006D.toInt(), 0xFF0048FF.toInt(), 0xFF249100.toInt(), 0xFF916D00.toInt(), 0xFFDA6D00.toInt(), 0xFF00B66D.toInt(), 0xFF6D6D6D.toInt(), 0xFF6D48DA.toInt(), 0xFF000000.toInt(), 0xFF0000DA.toInt(), 0xFFFF0000.toInt(), 0xFFB62400.toInt(), 0xFFFF91FF.toInt(), 0xFFFFB6B6.toInt(), 0xFFDA6DFF.toInt(), 0xFF004800.toInt(), 0xFF00006D.toInt(), 0xFFFFFF00.toInt(), 0xFF242424.toInt(), 0xFFFFB600.toInt(), 0xFFFF9100.toInt(), 0xFFFFFFFF.toInt(), 0xFF6DDA00.toInt(), 0xFF91006D.toInt(), 0xFF6DB6FF.toInt(), 0xFFFF00FF.toInt(), 0xFF006DDA.toInt(), 0xFF919191.toInt(), 0xFF000000.toInt(), 0xFF6D2400.toInt(), 0xFF00FFFF.toInt(), 0xFF480000.toInt(), 0xFFB6FF48.toInt(), 0xFFFFB6FF.toInt(), 0xFF914800.toInt(), 0xFF00FF00.toInt(), 0xFFDADA00.toInt(), 0xFF484848.toInt(), 0xFF006D24.toInt(), 0xFF000000.toInt(), 0xFFDAB6FF.toInt(), 0xFFFFFF6D.toInt(), 0xFF9100FF.toInt(), 0xFF48FFDA.toInt(), 0xFFFFDA00.toInt(), 0xFF004848.toInt()),
            /* 2C04-0004 */    intArrayOf(0xFF916D00.toInt(), 0xFF6D48DA.toInt(), 0xFF009191.toInt(), 0xFFDADA00.toInt(), 0xFF000000.toInt(), 0xFFFFB6B6.toInt(), 0xFF002491.toInt(), 0xFFDA6D00.toInt(), 0xFFB6B6B6.toInt(), 0xFF6D2400.toInt(), 0xFF00FF00.toInt(), 0xFF00006D.toInt(), 0xFFFFDA91.toInt(), 0xFFFFFF00.toInt(), 0xFF009100.toInt(), 0xFFB6FF48.toInt(), 0xFFFF6DFF.toInt(), 0xFF480000.toInt(), 0xFF0048FF.toInt(), 0xFFFF91FF.toInt(), 0xFF000000.toInt(), 0xFF484848.toInt(), 0xFFB62400.toInt(), 0xFFFF9100.toInt(), 0xFFDAB66D.toInt(), 0xFF00B66D.toInt(), 0xFF9191FF.toInt(), 0xFF249100.toInt(), 0xFF91006D.toInt(), 0xFF000000.toInt(), 0xFF91FF6D.toInt(), 0xFF6DB6FF.toInt(), 0xFFB6006D.toInt(), 0xFF006D24.toInt(), 0xFF914800.toInt(), 0xFF0000DA.toInt(), 0xFF9100FF.toInt(), 0xFFB600FF.toInt(), 0xFF6D6D6D.toInt(), 0xFFFF0091.toInt(), 0xFF004848.toInt(), 0xFFDADADA.toInt(), 0xFF006DDA.toInt(), 0xFF004800.toInt(), 0xFF242424.toInt(), 0xFFFFFF6D.toInt(), 0xFF919191.toInt(), 0xFFFF00FF.toInt(), 0xFFFFB6FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF6D4800.toInt(), 0xFFFF0000.toInt(), 0xFFFFDA00.toInt(), 0xFF48FFDA.toInt(), 0xFFFFFFFF.toInt(), 0xFF91DAFF.toInt(), 0xFF000000.toInt(), 0xFFFFB600.toInt(), 0xFFDA6DFF.toInt(), 0xFFB6DAFF.toInt(), 0xFF6DDA00.toInt(), 0xFFDAB6FF.toInt(), 0xFF00FFFF.toInt(), 0xFF244800.toInt()),
            /* 2C05-01 */      intArrayOf(0xFF6D6D6D.toInt(), 0xFF002491.toInt(), 0xFF0000DA.toInt(), 0xFF6D48DA.toInt(), 0xFF91006D.toInt(), 0xFFB6006D.toInt(), 0xFFB62400.toInt(), 0xFF914800.toInt(), 0xFF6D4800.toInt(), 0xFF244800.toInt(), 0xFF006D24.toInt(), 0xFF009100.toInt(), 0xFF004848.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFB6B6B6.toInt(), 0xFF006DDA.toInt(), 0xFF0048FF.toInt(), 0xFF9100FF.toInt(), 0xFFB600FF.toInt(), 0xFFFF0091.toInt(), 0xFFFF0000.toInt(), 0xFFDA6D00.toInt(), 0xFF916D00.toInt(), 0xFF249100.toInt(), 0xFF009100.toInt(), 0xFF00B66D.toInt(), 0xFF009191.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF6DB6FF.toInt(), 0xFF9191FF.toInt(), 0xFFDA6DFF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF6DFF.toInt(), 0xFFFF9100.toInt(), 0xFFFFB600.toInt(), 0xFFDADA00.toInt(), 0xFF6DDA00.toInt(), 0xFF00FF00.toInt(), 0xFF48FFDA.toInt(), 0xFF00FFFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFB6DAFF.toInt(), 0xFFDAB6FF.toInt(), 0xFFFFB6FF.toInt(), 0xFFFF91FF.toInt(), 0xFFFFB6B6.toInt(), 0xFFFFDA91.toInt(), 0xFFFFFF48.toInt(), 0xFFFFFF6D.toInt(), 0xFFB6FF48.toInt(), 0xFF91FF6D.toInt(), 0xFF48FFDA.toInt(), 0xFF91DAFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            /* 2C05-02 */      intArrayOf(0xFF6D6D6D.toInt(), 0xFF002491.toInt(), 0xFF0000DA.toInt(), 0xFF6D48DA.toInt(), 0xFF91006D.toInt(), 0xFFB6006D.toInt(), 0xFFB62400.toInt(), 0xFF914800.toInt(), 0xFF6D4800.toInt(), 0xFF244800.toInt(), 0xFF006D24.toInt(), 0xFF009100.toInt(), 0xFF004848.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFB6B6B6.toInt(), 0xFF006DDA.toInt(), 0xFF0048FF.toInt(), 0xFF9100FF.toInt(), 0xFFB600FF.toInt(), 0xFFFF0091.toInt(), 0xFFFF0000.toInt(), 0xFFDA6D00.toInt(), 0xFF916D00.toInt(), 0xFF249100.toInt(), 0xFF009100.toInt(), 0xFF00B66D.toInt(), 0xFF009191.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF6DB6FF.toInt(), 0xFF9191FF.toInt(), 0xFFDA6DFF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF6DFF.toInt(), 0xFFFF9100.toInt(), 0xFFFFB600.toInt(), 0xFFDADA00.toInt(), 0xFF6DDA00.toInt(), 0xFF00FF00.toInt(), 0xFF48FFDA.toInt(), 0xFF00FFFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFB6DAFF.toInt(), 0xFFDAB6FF.toInt(), 0xFFFFB6FF.toInt(), 0xFFFF91FF.toInt(), 0xFFFFB6B6.toInt(), 0xFFFFDA91.toInt(), 0xFFFFFF48.toInt(), 0xFFFFFF6D.toInt(), 0xFFB6FF48.toInt(), 0xFF91FF6D.toInt(), 0xFF48FFDA.toInt(), 0xFF91DAFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            /* 2C05-03 */      intArrayOf(0xFF6D6D6D.toInt(), 0xFF002491.toInt(), 0xFF0000DA.toInt(), 0xFF6D48DA.toInt(), 0xFF91006D.toInt(), 0xFFB6006D.toInt(), 0xFFB62400.toInt(), 0xFF914800.toInt(), 0xFF6D4800.toInt(), 0xFF244800.toInt(), 0xFF006D24.toInt(), 0xFF009100.toInt(), 0xFF004848.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFB6B6B6.toInt(), 0xFF006DDA.toInt(), 0xFF0048FF.toInt(), 0xFF9100FF.toInt(), 0xFFB600FF.toInt(), 0xFFFF0091.toInt(), 0xFFFF0000.toInt(), 0xFFDA6D00.toInt(), 0xFF916D00.toInt(), 0xFF249100.toInt(), 0xFF009100.toInt(), 0xFF00B66D.toInt(), 0xFF009191.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF6DB6FF.toInt(), 0xFF9191FF.toInt(), 0xFFDA6DFF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF6DFF.toInt(), 0xFFFF9100.toInt(), 0xFFFFB600.toInt(), 0xFFDADA00.toInt(), 0xFF6DDA00.toInt(), 0xFF00FF00.toInt(), 0xFF48FFDA.toInt(), 0xFF00FFFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFB6DAFF.toInt(), 0xFFDAB6FF.toInt(), 0xFFFFB6FF.toInt(), 0xFFFF91FF.toInt(), 0xFFFFB6B6.toInt(), 0xFFFFDA91.toInt(), 0xFFFFFF48.toInt(), 0xFFFFFF6D.toInt(), 0xFFB6FF48.toInt(), 0xFF91FF6D.toInt(), 0xFF48FFDA.toInt(), 0xFF91DAFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            /* 2C05-04 */      intArrayOf(0xFF6D6D6D.toInt(), 0xFF002491.toInt(), 0xFF0000DA.toInt(), 0xFF6D48DA.toInt(), 0xFF91006D.toInt(), 0xFFB6006D.toInt(), 0xFFB62400.toInt(), 0xFF914800.toInt(), 0xFF6D4800.toInt(), 0xFF244800.toInt(), 0xFF006D24.toInt(), 0xFF009100.toInt(), 0xFF004848.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFB6B6B6.toInt(), 0xFF006DDA.toInt(), 0xFF0048FF.toInt(), 0xFF9100FF.toInt(), 0xFFB600FF.toInt(), 0xFFFF0091.toInt(), 0xFFFF0000.toInt(), 0xFFDA6D00.toInt(), 0xFF916D00.toInt(), 0xFF249100.toInt(), 0xFF009100.toInt(), 0xFF00B66D.toInt(), 0xFF009191.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF6DB6FF.toInt(), 0xFF9191FF.toInt(), 0xFFDA6DFF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF6DFF.toInt(), 0xFFFF9100.toInt(), 0xFFFFB600.toInt(), 0xFFDADA00.toInt(), 0xFF6DDA00.toInt(), 0xFF00FF00.toInt(), 0xFF48FFDA.toInt(), 0xFF00FFFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFB6DAFF.toInt(), 0xFFDAB6FF.toInt(), 0xFFFFB6FF.toInt(), 0xFFFF91FF.toInt(), 0xFFFFB6B6.toInt(), 0xFFFFDA91.toInt(), 0xFFFFFF48.toInt(), 0xFFFFFF6D.toInt(), 0xFFB6FF48.toInt(), 0xFF91FF6D.toInt(), 0xFF48FFDA.toInt(), 0xFF91DAFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            /* 2C05-05 */      intArrayOf(0xFF6D6D6D.toInt(), 0xFF002491.toInt(), 0xFF0000DA.toInt(), 0xFF6D48DA.toInt(), 0xFF91006D.toInt(), 0xFFB6006D.toInt(), 0xFFB62400.toInt(), 0xFF914800.toInt(), 0xFF6D4800.toInt(), 0xFF244800.toInt(), 0xFF006D24.toInt(), 0xFF009100.toInt(), 0xFF004848.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFB6B6B6.toInt(), 0xFF006DDA.toInt(), 0xFF0048FF.toInt(), 0xFF9100FF.toInt(), 0xFFB600FF.toInt(), 0xFFFF0091.toInt(), 0xFFFF0000.toInt(), 0xFFDA6D00.toInt(), 0xFF916D00.toInt(), 0xFF249100.toInt(), 0xFF009100.toInt(), 0xFF00B66D.toInt(), 0xFF009191.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF6DB6FF.toInt(), 0xFF9191FF.toInt(), 0xFFDA6DFF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF6DFF.toInt(), 0xFFFF9100.toInt(), 0xFFFFB600.toInt(), 0xFFDADA00.toInt(), 0xFF6DDA00.toInt(), 0xFF00FF00.toInt(), 0xFF48FFDA.toInt(), 0xFF00FFFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFB6DAFF.toInt(), 0xFFDAB6FF.toInt(), 0xFFFFB6FF.toInt(), 0xFFFF91FF.toInt(), 0xFFFFB6B6.toInt(), 0xFFFFDA91.toInt(), 0xFFFFFF48.toInt(), 0xFFFFFF6D.toInt(), 0xFFB6FF48.toInt(), 0xFF91FF6D.toInt(), 0xFF48FFDA.toInt(), 0xFF91DAFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
        )
        // @formatter:on

        internal val DEFAULT_PALETTE = intArrayOf(
            0xFF666666.toInt(), 0xFF002A88.toInt(), 0xFF1412A7.toInt(), 0xFF3B00A4.toInt(),
            0xFF5C007E.toInt(), 0xFF6E0040.toInt(), 0xFF6C0600.toInt(), 0xFF561D00.toInt(),
            0xFF333500.toInt(), 0xFF0B4800.toInt(), 0xFF005200.toInt(), 0xFF004F08.toInt(),
            0xFF00404D.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFADADAD.toInt(), 0xFF155FD9.toInt(), 0xFF4240FF.toInt(), 0xFF7527FE.toInt(),
            0xFFA01ACC.toInt(), 0xFFB71E7B.toInt(), 0xFFB53120.toInt(), 0xFF994E00.toInt(),
            0xFF6B6D00.toInt(), 0xFF388700.toInt(), 0xFF0C9300.toInt(), 0xFF008F32.toInt(),
            0xFF007C8D.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFEFF.toInt(), 0xFF64B0FF.toInt(), 0xFF9290FF.toInt(), 0xFFC676FF.toInt(),
            0xFFF36AFF.toInt(), 0xFFFE6ECC.toInt(), 0xFFFE8170.toInt(), 0xFFEA9E22.toInt(),
            0xFFBCBE00.toInt(), 0xFF88D800.toInt(), 0xFF5CE430.toInt(), 0xFF45E082.toInt(),
            0xFF48CDDE.toInt(), 0xFF4F4F4F.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFEFF.toInt(), 0xFFC0DFFF.toInt(), 0xFFD3D2FF.toInt(), 0xFFE8C8FF.toInt(),
            0xFFFBC2FF.toInt(), 0xFFFEC4EA.toInt(), 0xFFFECCC5.toInt(), 0xFFF7D8A5.toInt(),
            0xFFE4E594.toInt(), 0xFFCFEF96.toInt(), 0xFFBDF4AB.toInt(), 0xFFB3F3CC.toInt(),
            0xFFB5EBF2.toInt(), 0xFFB8B8B8.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )

        internal val UNSATURATED_PALETTE = intArrayOf(
            0xFF6B6B6B.toInt(), 0xFF001E87.toInt(), 0xFF1F0B96.toInt(), 0xFF3B0C87.toInt(),
            0xFF590D61.toInt(), 0xFF5E0528.toInt(), 0xFF551100.toInt(), 0xFF461B00.toInt(),
            0xFF303200.toInt(), 0xFF0A4800.toInt(), 0xFF004E00.toInt(), 0xFF004619.toInt(),
            0xFF003A58.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFB2B2B2.toInt(), 0xFF1A53D1.toInt(), 0xFF4835EE.toInt(), 0xFF7123EC.toInt(),
            0xFF9A1EB7.toInt(), 0xFFA51E62.toInt(), 0xFFA52D19.toInt(), 0xFF874B00.toInt(),
            0xFF676900.toInt(), 0xFF298400.toInt(), 0xFF038B00.toInt(), 0xFF008240.toInt(),
            0xFF007891.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFF63ADFD.toInt(), 0xFF908AFE.toInt(), 0xFFB977FC.toInt(),
            0xFFE771FE.toInt(), 0xFFF76FC9.toInt(), 0xFFF5836A.toInt(), 0xFFDD9C29.toInt(),
            0xFFBDB807.toInt(), 0xFF84D107.toInt(), 0xFF5BDC3B.toInt(), 0xFF48D77D.toInt(),
            0xFF48CCCE.toInt(), 0xFF555555.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFFC4E3FE.toInt(), 0xFFD7D5FE.toInt(), 0xFFE6CDFE.toInt(),
            0xFFF9CAFE.toInt(), 0xFFFEC9F0.toInt(), 0xFFFED1C7.toInt(), 0xFFF7DCAC.toInt(),
            0xFFE8E89C.toInt(), 0xFFD1F29D.toInt(), 0xFFBFF4B1.toInt(), 0xFFB7F5CD.toInt(),
            0xFFB7F0EE.toInt(), 0xFFBEBEBE.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )

        internal val YUV_PALETTE = intArrayOf(
            0xFF666666.toInt(), 0xFF002A88.toInt(), 0xFF1412A7.toInt(), 0xFF3B00A4.toInt(),
            0xFF5C007E.toInt(), 0xFF6E0040.toInt(), 0xFF6C0700.toInt(), 0xFF561D00.toInt(),
            0xFF333500.toInt(), 0xFF0C4800.toInt(), 0xFF005200.toInt(), 0xFF004C18.toInt(),
            0xFF003E5B.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFADADAD.toInt(), 0xFF155FD9.toInt(), 0xFF4240FF.toInt(), 0xFF7527FE.toInt(),
            0xFFA01ACC.toInt(), 0xFFB71E7B.toInt(), 0xFFB53120.toInt(), 0xFF994E00.toInt(),
            0xFF6B6D00.toInt(), 0xFF388700.toInt(), 0xFF0D9300.toInt(), 0xFF008C47.toInt(),
            0xFF007AA0.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFF64B0FF.toInt(), 0xFF9290FF.toInt(), 0xFFC676FF.toInt(),
            0xFFF26AFF.toInt(), 0xFFFF6ECC.toInt(), 0xFFFF8170.toInt(), 0xFFEA9E22.toInt(),
            0xFFBCBE00.toInt(), 0xFF88D800.toInt(), 0xFF5CE430.toInt(), 0xFF45E082.toInt(),
            0xFF48CDDE.toInt(), 0xFF4F4F4F.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFFC0DFFF.toInt(), 0xFFD3D2FF.toInt(), 0xFFE8C8FF.toInt(),
            0xFFFAC2FF.toInt(), 0xFFFFC4EA.toInt(), 0xFFFFCCC5.toInt(), 0xFFF7D8A5.toInt(),
            0xFFE4E594.toInt(), 0xFFCFEF96.toInt(), 0xFFBDF4AB.toInt(), 0xFFB3F3CC.toInt(),
            0xFFB5EBF2.toInt(), 0xFFB8B8B8.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )

        internal val NESTOPIA_PALETTE = intArrayOf(
            0xFF6D6D6D.toInt(), 0xFF002492.toInt(), 0xFF0000DB.toInt(), 0xFF6D49DB.toInt(),
            0xFF92006D.toInt(), 0xFFB6006D.toInt(), 0xFFB62400.toInt(), 0xFF924900.toInt(),
            0xFF6D4900.toInt(), 0xFF244900.toInt(), 0xFF006D24.toInt(), 0xFF009200.toInt(),
            0xFF004949.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFB6B6B6.toInt(), 0xFF006DDB.toInt(), 0xFF0049FF.toInt(), 0xFF9200FF.toInt(),
            0xFFB600FF.toInt(), 0xFFFF0092.toInt(), 0xFFFF0000.toInt(), 0xFFDB6D00.toInt(),
            0xFF926D00.toInt(), 0xFF249200.toInt(), 0xFF009200.toInt(), 0xFF00B66D.toInt(),
            0xFF009292.toInt(), 0xFF242424.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFF6DB6FF.toInt(), 0xFF9292FF.toInt(), 0xFFDB6DFF.toInt(),
            0xFFFF00FF.toInt(), 0xFFFF6DFF.toInt(), 0xFFFF9200.toInt(), 0xFFFFB600.toInt(),
            0xFFDBDB00.toInt(), 0xFF6DDB00.toInt(), 0xFF00FF00.toInt(), 0xFF49FFDB.toInt(),
            0xFF00FFFF.toInt(), 0xFF494949.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFFB6DBFF.toInt(), 0xFFDBB6FF.toInt(), 0xFFFFB6FF.toInt(),
            0xFFFF92FF.toInt(), 0xFFFFB6B6.toInt(), 0xFFFFDB92.toInt(), 0xFFFFFF49.toInt(),
            0xFFFFFF6D.toInt(), 0xFFB6FF49.toInt(), 0xFF92FF6D.toInt(), 0xFF49FFDB.toInt(),
            0xFF92DBFF.toInt(), 0xFF929292.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )

        internal val COMPOSITE_DIRECT_PALETTE = intArrayOf(
            0xFF656565.toInt(), 0xFF00127D.toInt(), 0xFF18008E.toInt(), 0xFF360082.toInt(),
            0xFF56005D.toInt(), 0xFF5A0018.toInt(), 0xFF4F0500.toInt(), 0xFF381900.toInt(),
            0xFF1D3100.toInt(), 0xFF003D00.toInt(), 0xFF004100.toInt(), 0xFF003B17.toInt(),
            0xFF002E55.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFAFAFAF.toInt(), 0xFF194EC8.toInt(), 0xFF472FE3.toInt(), 0xFF6B1FD7.toInt(),
            0xFF931BAE.toInt(), 0xFF9E1A5E.toInt(), 0xFF993200.toInt(), 0xFF7B4B00.toInt(),
            0xFF5B6700.toInt(), 0xFF267A00.toInt(), 0xFF008200.toInt(), 0xFF007A3E.toInt(),
            0xFF006E8A.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFF64A9FF.toInt(), 0xFF8E89FF.toInt(), 0xFFB676FF.toInt(),
            0xFFE06FFF.toInt(), 0xFFEF6CC4.toInt(), 0xFFF0806A.toInt(), 0xFFD8982C.toInt(),
            0xFFB9B40A.toInt(), 0xFF83CB0C.toInt(), 0xFF5BD63F.toInt(), 0xFF4AD17E.toInt(),
            0xFF4DC7CB.toInt(), 0xFF4C4C4C.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFFC7E5FF.toInt(), 0xFFD9D9FF.toInt(), 0xFFE9D1FF.toInt(),
            0xFFF9CEFF.toInt(), 0xFFFFCCF1.toInt(), 0xFFFFD4CB.toInt(), 0xFFF8DFB1.toInt(),
            0xFFEDEAA4.toInt(), 0xFFD6F4A4.toInt(), 0xFFC5F8B8.toInt(), 0xFFBEF6D3.toInt(),
            0xFFBFF1F1.toInt(), 0xFFB9B9B9.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )

        internal val NES_CLASSIC_PALETTE = intArrayOf(
            0xFF60615F.toInt(), 0xFF000083.toInt(), 0xFF1D0195.toInt(), 0xFF340875.toInt(),
            0xFF51055E.toInt(), 0xFF56000F.toInt(), 0xFF4C0700.toInt(), 0xFF372308.toInt(),
            0xFF203A0B.toInt(), 0xFF0F4B0E.toInt(), 0xFF194C16.toInt(), 0xFF02421E.toInt(),
            0xFF023154.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFA9AAA8.toInt(), 0xFF104BBF.toInt(), 0xFF4712D8.toInt(), 0xFF6300CA.toInt(),
            0xFF8800A9.toInt(), 0xFF930B46.toInt(), 0xFF8A2D04.toInt(), 0xFF6F5206.toInt(),
            0xFF5C7114.toInt(), 0xFF1B8D12.toInt(), 0xFF199509.toInt(), 0xFF178448.toInt(),
            0xFF206B8E.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFBFBFB.toInt(), 0xFF6699F8.toInt(), 0xFF8974F9.toInt(), 0xFFAB58F8.toInt(),
            0xFFD557EF.toInt(), 0xFFDE5FA9.toInt(), 0xFFDC7F59.toInt(), 0xFFC7A224.toInt(),
            0xFFA7BE03.toInt(), 0xFF75D703.toInt(), 0xFF60E34F.toInt(), 0xFF3CD68D.toInt(),
            0xFF56C9CC.toInt(), 0xFF414240.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFBFBFB.toInt(), 0xFFBED4FA.toInt(), 0xFFC9C7F9.toInt(), 0xFFD7BEFA.toInt(),
            0xFFE8B8F9.toInt(), 0xFFF5BAE5.toInt(), 0xFFF3CAC2.toInt(), 0xFFDFCDA7.toInt(),
            0xFFD9E09C.toInt(), 0xFFC9EB9E.toInt(), 0xFFC0EDB8.toInt(), 0xFFB5F4C7.toInt(),
            0xFFB9EAE9.toInt(), 0xFFABABAB.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )

        internal val ORIGINAL_HARDWARE_PALETTE = intArrayOf(
            0xFF6A6D6A.toInt(), 0xFF00127D.toInt(), 0xFF1E008A.toInt(), 0xFF3B007D.toInt(),
            0xFF56005D.toInt(), 0xFF5A0018.toInt(), 0xFF4F0D00.toInt(), 0xFF381E00.toInt(),
            0xFF203100.toInt(), 0xFF003D00.toInt(), 0xFF004000.toInt(), 0xFF003B1E.toInt(),
            0xFF002E55.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFB9BCB9.toInt(), 0xFF194EC8.toInt(), 0xFF472FE3.toInt(), 0xFF751FD7.toInt(),
            0xFF931EAD.toInt(), 0xFF9E245E.toInt(), 0xFF963800.toInt(), 0xFF7B5000.toInt(),
            0xFF5B6700.toInt(), 0xFF267A00.toInt(), 0xFF007F00.toInt(), 0xFF007842.toInt(),
            0xFF006E8A.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFF69AEFF.toInt(), 0xFF9798FF.toInt(), 0xFFB687FF.toInt(),
            0xFFE278FF.toInt(), 0xFFF279C7.toInt(), 0xFFF58F6F.toInt(), 0xFFDDA932.toInt(),
            0xFFBCB70D.toInt(), 0xFF88D015.toInt(), 0xFF60DB49.toInt(), 0xFF4FD687.toInt(),
            0xFF50CACE.toInt(), 0xFF515451.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFFCCEAFF.toInt(), 0xFFDEE2FF.toInt(), 0xFFEEDAFF.toInt(),
            0xFFFAD7FD.toInt(), 0xFFFDD7F6.toInt(), 0xFFFDDCD0.toInt(), 0xFFFAE8B6.toInt(),
            0xFFF2F1A9.toInt(), 0xFFDBFBA9.toInt(), 0xFFCAFFBD.toInt(), 0xFFC3FBD8.toInt(),
            0xFFC4F6F6.toInt(), 0xFFBEC1BE.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )

        internal val PVM_STYLE_PALETTE = intArrayOf(
            0xFF696964.toInt(), 0xFF001774.toInt(), 0xFF28007D.toInt(), 0xFF3E006D.toInt(),
            0xFF560057.toInt(), 0xFF5E0013.toInt(), 0xFF531A00.toInt(), 0xFF3B2400.toInt(),
            0xFF2A3000.toInt(), 0xFF143A00.toInt(), 0xFF003F00.toInt(), 0xFF003B1E.toInt(),
            0xFF003050.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFB9B9B4.toInt(), 0xFF1453B9.toInt(), 0xFF4D2CDA.toInt(), 0xFF7A1EC8.toInt(),
            0xFF98189C.toInt(), 0xFF9D2344.toInt(), 0xFFA03E00.toInt(), 0xFF8D5500.toInt(),
            0xFF656D00.toInt(), 0xFF2C7900.toInt(), 0xFF008100.toInt(), 0xFF007D42.toInt(),
            0xFF00788A.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFF69A8FF.toInt(), 0xFF9A96FF.toInt(), 0xFFC28AFA.toInt(),
            0xFFEA7DFA.toInt(), 0xFFF387B4.toInt(), 0xFFF1986C.toInt(), 0xFFE6B327.toInt(),
            0xFFD7C805.toInt(), 0xFF90DF07.toInt(), 0xFF64E53C.toInt(), 0xFF45E27D.toInt(),
            0xFF48D5D9.toInt(), 0xFF4B4B46.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFFD2EAFF.toInt(), 0xFFE2E2FF.toInt(), 0xFFF2D8FF.toInt(),
            0xFFF8D2FF.toInt(), 0xFFF8D9EA.toInt(), 0xFFFADEB9.toInt(), 0xFFF9E89B.toInt(),
            0xFFF3F28C.toInt(), 0xFFD3FA91.toInt(), 0xFFB8FCA8.toInt(), 0xFFAEFACA.toInt(),
            0xFFCAF3F3.toInt(), 0xFFBEBEB9.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )

        internal val SONY_CXA_2025_PALETTE = intArrayOf(
            0xFF585858.toInt(), 0xFF00238C.toInt(), 0xFF00139B.toInt(), 0xFF2D0585.toInt(),
            0xFF5D0052.toInt(), 0xFF7A0017.toInt(), 0xFF7A0800.toInt(), 0xFF5F1800.toInt(),
            0xFF352A00.toInt(), 0xFF093900.toInt(), 0xFF003F00.toInt(), 0xFF003C22.toInt(),
            0xFF00325D.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFA1A1A1.toInt(), 0xFF0053EE.toInt(), 0xFF153CFE.toInt(), 0xFF6028E4.toInt(),
            0xFFA91D98.toInt(), 0xFFD41E41.toInt(), 0xFFD22C00.toInt(), 0xFFAA4400.toInt(),
            0xFF6C5E00.toInt(), 0xFF2D7300.toInt(), 0xFF007D06.toInt(), 0xFF007852.toInt(),
            0xFF0069A9.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFF1FA5FE.toInt(), 0xFF5E89FE.toInt(), 0xFFB572FE.toInt(),
            0xFFFE65F6.toInt(), 0xFFFE6790.toInt(), 0xFFFE773C.toInt(), 0xFFFE9308.toInt(),
            0xFFC4B200.toInt(), 0xFF79CA10.toInt(), 0xFF3AD54A.toInt(), 0xFF11D1A4.toInt(),
            0xFF06BFFE.toInt(), 0xFF424242.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFFA0D9FE.toInt(), 0xFFBDCCFE.toInt(), 0xFFE1C2FE.toInt(),
            0xFFFEBCFB.toInt(), 0xFFFEBDD0.toInt(), 0xFFFEC5A9.toInt(), 0xFFFED18E.toInt(),
            0xFFE9DE86.toInt(), 0xFFC7E992.toInt(), 0xFFA8EEB0.toInt(), 0xFF95ECD9.toInt(),
            0xFF91E4FE.toInt(), 0xFFACACAC.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )

        internal val WAVEBEAM_PALETTE = intArrayOf(
            0xFF6B6B6B.toInt(), 0xFF001B88.toInt(), 0xFF21009A.toInt(), 0xFF40008C.toInt(),
            0xFF600067.toInt(), 0xFF64001E.toInt(), 0xFF590800.toInt(), 0xFF481600.toInt(),
            0xFF283600.toInt(), 0xFF004500.toInt(), 0xFF004908.toInt(), 0xFF00421D.toInt(),
            0xFF003659.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFB4B4B4.toInt(), 0xFF1555D3.toInt(), 0xFF4337EF.toInt(), 0xFF7425DF.toInt(),
            0xFF9C19B9.toInt(), 0xFFAC0F64.toInt(), 0xFFAA2C00.toInt(), 0xFF8A4B00.toInt(),
            0xFF666B00.toInt(), 0xFF218300.toInt(), 0xFF008A00.toInt(), 0xFF008144.toInt(),
            0xFF007691.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFF63B2FF.toInt(), 0xFF7C9CFF.toInt(), 0xFFC07DFE.toInt(),
            0xFFE977FF.toInt(), 0xFFF572CD.toInt(), 0xFFF4886B.toInt(), 0xFFDDA029.toInt(),
            0xFFBDBD0A.toInt(), 0xFF89D20E.toInt(), 0xFF5CDE3E.toInt(), 0xFF4BD886.toInt(),
            0xFF4DCFD2.toInt(), 0xFF525252.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
            0xFFFFFFFF.toInt(), 0xFFBCDFFF.toInt(), 0xFFD2D2FF.toInt(), 0xFFE1C8FF.toInt(),
            0xFFEFC7FF.toInt(), 0xFFFFC3E1.toInt(), 0xFFFFCAC6.toInt(), 0xFFF2DAAD.toInt(),
            0xFFEBE3A0.toInt(), 0xFFD2EDA2.toInt(), 0xFFBCF4B4.toInt(), 0xFFB5F1CE.toInt(),
            0xFFB6ECF1.toInt(), 0xFFBFBFBF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),
        )
    }
}
