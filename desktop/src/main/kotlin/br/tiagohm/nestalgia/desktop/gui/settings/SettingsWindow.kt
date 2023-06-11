package br.tiagohm.nestalgia.desktop.gui.settings

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.ConsoleType.*
import br.tiagohm.nestalgia.core.ExpansionPortDevice.*
import br.tiagohm.nestalgia.desktop.app.Preferences
import br.tiagohm.nestalgia.desktop.gui.AbstractDialog
import br.tiagohm.nestalgia.desktop.gui.settings.controllers.AsciiTurboFileSettingsWindow
import br.tiagohm.nestalgia.desktop.gui.settings.controllers.StandardControllerSettingsWindow
import br.tiagohm.nestalgia.desktop.gui.settings.controllers.ZapperSettingsWindow
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Spinner
import javafx.scene.layout.Pane
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.stereotype.Component

@Component
class SettingsWindow : AbstractDialog() {

    override val resourceName = "Settings"

    @Autowired private lateinit var preferences: Preferences
    @Autowired private lateinit var beanFactory: AutowireCapableBeanFactory

    // Controller.
    @FXML private lateinit var automaticallyConfigureControllersWhenLoadingGameCheckBox: CheckBox
    @FXML private lateinit var consoleTypeChoiceBox: ChoiceBox<ConsoleType>
    @FXML private lateinit var player1ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var player2ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var player3ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var player4ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var expansionPortNesChoiceBox: ChoiceBox<ExpansionPortDevice>
    @FXML private lateinit var expansionPortFamicomChoiceBox: ChoiceBox<ExpansionPortDevice>
    @FXML private lateinit var player3Box: Pane
    @FXML private lateinit var player4Box: Pane
    @FXML private lateinit var expansionPortNesBox: Pane
    @FXML private lateinit var expansionPortFamicomBox: Pane

    // Audio.
    @FXML private lateinit var sampleRateChoiceBox: ChoiceBox<String>
    @FXML private lateinit var disableNoiseChannelModeFlagCheckBox: CheckBox
    @FXML private lateinit var muteUltrasonicFrequenciesOnTriangleChannelCheckBox: CheckBox
    @FXML private lateinit var swapSquareChannelsDutyCyclesCheckBox: CheckBox
    @FXML private lateinit var reducePoppingSoundsOnTheDMCChannelCheckBox: CheckBox

    // Video.
    @FXML private lateinit var enableIntegerFPSModeCheckBox: CheckBox
    @FXML private lateinit var paletteChoiceBox: ChoiceBox<PaletteType>
    @FXML private lateinit var removeSpriteLimitCheckBox: CheckBox
    @FXML private lateinit var autoReenableSpriteLimitAsNeededCheckBox: CheckBox
    @FXML private lateinit var forceSpriteDisplayInFirstColumnCheckBox: CheckBox
    @FXML private lateinit var forceBackgroundDisplayInFirstColumnCheckBox: CheckBox
    @FXML private lateinit var disableSpritesCheckBox: CheckBox
    @FXML private lateinit var disableBackgroundCheckBox: CheckBox

    // Emulation.
    @FXML private lateinit var enablePPUOAMRowCorruptionEmulationCheckBox: CheckBox
    @FXML private lateinit var enablePPU200020052006FirstWriteScrollGlitchEmulationCheckBox: CheckBox
    @FXML private lateinit var enablePPU2006WriteScrollGlitchEmulationCheckBox: CheckBox
    @FXML private lateinit var randomizePowerOnCPUPPUAlignmentCheckBox: CheckBox
    @FXML private lateinit var randomizePowerOnStateForMappersCheckBox: CheckBox
    @FXML private lateinit var defaultPowerOnStateForRAMChoiceBox: ChoiceBox<RamPowerOnState>
    @FXML private lateinit var enableOAMRAMDecayCheckBox: CheckBox
    @FXML private lateinit var disablePPUPaletteReadsCheckBox: CheckBox
    @FXML private lateinit var disablePPUOAMADDRBugEmulationCheckBox: CheckBox
    @FXML private lateinit var doNotResetPPUWhenResettingConsoleCheckBox: CheckBox
    @FXML private lateinit var disablePPU2004ReadsCheckBox: CheckBox
    @FXML private lateinit var useNESHVC101BehaviourCheckBox: CheckBox
    @FXML private lateinit var useAlternativeMMC3IRQBehaviourCheckBox: CheckBox
    @FXML private lateinit var allowInvalidInputCheckBox: CheckBox
    @FXML private lateinit var additionalScanlinesBeforeNMISpinner: Spinner<Double>
    @FXML private lateinit var additionalScanlinesAfterNMISpinner: Spinner<Double>

    // FDS.
    @FXML private lateinit var autoInsertDisk1SideAWhenStartingCheckBox: CheckBox
    @FXML private lateinit var autoSwitchDisksCheckBox: CheckBox

    private val initialState = Snapshot()
    private val controllerKeys = Array(8) { KeyMapping() }
    private val zapperDetectionRadius = IntArray(8) { 1 }
    private var reset = false

    val settings
        get() = preferences.emulationSettings

    var consoleType
        get() = consoleTypeChoiceBox.value ?: NES
        set(value) {
            consoleTypeChoiceBox.value = value
        }

    val hasFourScore
        get() = expansionPortDevice == FOUR_PLAYER_ADAPTER

    val expansionPortChoiceBox
        get() = if (consoleType == NES) expansionPortNesChoiceBox else expansionPortFamicomChoiceBox

    var expansionPortDevice
        get() = expansionPortChoiceBox.value ?: NONE
        set(value) {
            expansionPortChoiceBox.value = value
        }

    var controllerTypePlayer1
        get() = player1ChoiceBox.value ?: ControllerType.NONE
        set(value) {
            player1ChoiceBox.value = value
        }

    var controllerTypePlayer2
        get() = player2ChoiceBox.value ?: ControllerType.NONE
        set(value) {
            player2ChoiceBox.value = value
        }

    var controllerTypePlayer3
        get() = player3ChoiceBox.value ?: ControllerType.NONE
        set(value) {
            player3ChoiceBox.value = value
        }

    var controllerTypePlayer4
        get() = player4ChoiceBox.value ?: ControllerType.NONE
        set(value) {
            player4ChoiceBox.value = value
        }

    override fun onCreate() {
        title = "Settings"
        resizable = false
    }

    override fun onStart() {
        super.onStart()

        if (!reset) {
            settings.saveState(initialState)
        }

        reset = false

        automaticallyConfigureControllersWhenLoadingGameCheckBox.isSelected = settings.flag(EmulationFlag.AUTO_CONFIGURE_INPUT)
        consoleType = settings.consoleType

        if (consoleType == NES) {
            expansionPortNesChoiceBox.value = settings.expansionPortDevice
            expansionPortFamicomChoiceBox.value = NONE
        } else {
            expansionPortFamicomChoiceBox.value = settings.expansionPortDevice
            expansionPortNesChoiceBox.value = NONE
        }

        repeat(4) { settings.controllerKeys(it).copyTo(controllerKeys[it]) }
        settings.zapperDetectionRadius.copyInto(zapperDetectionRadius)

        expansionPortNesBox.visibleProperty().bind(consoleTypeChoiceBox.valueProperty().isEqualTo(NES))
        expansionPortNesBox.managedProperty().bind(expansionPortNesBox.visibleProperty())
        expansionPortFamicomBox.visibleProperty().bind(consoleTypeChoiceBox.valueProperty().isEqualTo(FAMICOM))
        expansionPortFamicomBox.managedProperty().bind(expansionPortFamicomBox.visibleProperty())

        sampleRateChoiceBox.value = settings.sampleRate.toString()
        disableNoiseChannelModeFlagCheckBox.isSelected = settings.flag(EmulationFlag.DISABLE_NOISE_MODE_FLAG)
        muteUltrasonicFrequenciesOnTriangleChannelCheckBox.isSelected = settings.flag(EmulationFlag.SILENCE_TRIANGLE_HIGH_FREQ)
        swapSquareChannelsDutyCyclesCheckBox.isSelected = settings.flag(EmulationFlag.SWAP_DUTY_CYCLES)
        reducePoppingSoundsOnTheDMCChannelCheckBox.isSelected = settings.flag(EmulationFlag.REDUCE_DMC_POPPING)

        enableIntegerFPSModeCheckBox.isSelected = settings.flag(EmulationFlag.INTEGER_FPS_MODE)
        paletteChoiceBox.value = settings.paletteType
        removeSpriteLimitCheckBox.isSelected = settings.flag(EmulationFlag.REMOVE_SPRITE_LIMIT)
        autoReenableSpriteLimitAsNeededCheckBox.isSelected = settings.flag(EmulationFlag.ADAPTIVE_SPRITE_LIMIT)
        autoReenableSpriteLimitAsNeededCheckBox.disableProperty().bind(!removeSpriteLimitCheckBox.selectedProperty())
        forceSpriteDisplayInFirstColumnCheckBox.isSelected = settings.flag(EmulationFlag.FORCE_SPRITES_FIRST_COLUMN)
        forceBackgroundDisplayInFirstColumnCheckBox.isSelected = settings.flag(EmulationFlag.FORCE_BACKGROUND_FIRST_COLUMN)
        disableSpritesCheckBox.isSelected = settings.flag(EmulationFlag.DISABLE_SPRITES)
        disableBackgroundCheckBox.isSelected = settings.flag(EmulationFlag.DISABLE_BACKGROUND)

        enablePPUOAMRowCorruptionEmulationCheckBox.isSelected = settings.flag(EmulationFlag.ENABLE_PPU_OAM_ROW_CORRUPTION)
        enablePPU200020052006FirstWriteScrollGlitchEmulationCheckBox.isSelected = settings.flag(EmulationFlag.ENABLE_PPU_2000_SCROLL_GLITCH)
        enablePPU2006WriteScrollGlitchEmulationCheckBox.isSelected = settings.flag(EmulationFlag.ENABLE_PPU_2006_SCROLL_GLITCH)
        randomizePowerOnCPUPPUAlignmentCheckBox.isSelected = settings.flag(EmulationFlag.RANDOMIZE_CPU_PPU_ALIGNMENT)
        randomizePowerOnStateForMappersCheckBox.isSelected = settings.flag(EmulationFlag.RANDOMIZE_MAPPER_POWER_ON_STATE)
        defaultPowerOnStateForRAMChoiceBox.value = settings.ramPowerOnState
        enableOAMRAMDecayCheckBox.isSelected = settings.flag(EmulationFlag.ENABLE_OAM_DECAY)
        disablePPUPaletteReadsCheckBox.isSelected = settings.flag(EmulationFlag.DISABLE_PALETTE_READ)
        disablePPUOAMADDRBugEmulationCheckBox.isSelected = settings.flag(EmulationFlag.DISABLE_OAM_ADDR_BUG)
        doNotResetPPUWhenResettingConsoleCheckBox.isSelected = settings.flag(EmulationFlag.DISABLE_PPU_RESET)
        disablePPU2004ReadsCheckBox.isSelected = settings.flag(EmulationFlag.DISABLE_PPU_2004_READS)
        useNESHVC101BehaviourCheckBox.isSelected = settings.flag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)
        useAlternativeMMC3IRQBehaviourCheckBox.isSelected = settings.flag(EmulationFlag.MMC3_IRQ_ALT_BEHAVIOR)
        allowInvalidInputCheckBox.isSelected = settings.flag(EmulationFlag.ALLOW_INVALID_INPUT)
        additionalScanlinesBeforeNMISpinner.valueFactory.value = settings.extraScanlinesBeforeNmi.toDouble()
        additionalScanlinesAfterNMISpinner.valueFactory.value = settings.extraScanlinesAfterNmi.toDouble()

        autoInsertDisk1SideAWhenStartingCheckBox.isSelected = settings.flag(EmulationFlag.FDS_AUTO_LOAD_DISK)
        autoSwitchDisksCheckBox.isSelected = settings.flag(EmulationFlag.FDS_AUTO_INSERT_DISK)

        consoleTypeChoiceBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updatePlayerOptions()
        }

        expansionPortNesChoiceBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updatePlayerOptions()
        }

        expansionPortFamicomChoiceBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updatePlayerOptions()
        }

        updatePlayerOptions()
    }

    override fun onStop() {
        if (!saved) {
            cancel(null)
        }
    }

    private fun updatePlayerOptions() {
        player1ChoiceBox.items.clear()
        player2ChoiceBox.items.clear()
        player3ChoiceBox.items.clear()
        player4ChoiceBox.items.clear()

        val isNes = consoleType == NES
        val isOriginalFamicom = !isNes && !settings.flag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)

        player3ChoiceBox.items.add(ControllerType.NONE)
        player3ChoiceBox.items.add(ControllerType.STANDARD)
        // player3ChoiceBox.items.add(ControllerType.SNES_MOUSE)
        // player3ChoiceBox.items.add(ControllerType.SUBOR_MOUSE)

        player4ChoiceBox.items.add(ControllerType.NONE)
        player4ChoiceBox.items.add(ControllerType.STANDARD)
        // player4ChoiceBox.items.add(ControllerType.SNES_MOUSE)
        // player4ChoiceBox.items.add(ControllerType.SUBOR_MOUSE)

        // if (!isNes) {
        // player3ChoiceBox.items.add(ControllerType.SNES)
        // player4ChoiceBox.items.add(ControllerType.SNES)
        // }

        if (isOriginalFamicom) {
            player1ChoiceBox.items.add(ControllerType.STANDARD)
            player2ChoiceBox.items.add(ControllerType.STANDARD)
        } else if (isNes && !hasFourScore) {
            player1ChoiceBox.items.addAll(player3ChoiceBox.items)
            player1ChoiceBox.items.add(ControllerType.ZAPPER)

            player2ChoiceBox.items.addAll(player4ChoiceBox.items)
            player2ChoiceBox.items.add(ControllerType.ZAPPER)

            // with(listOf(ControllerType.ARKANOID, ControllerType.POWER_PAD, ControllerType.SNES, ControllerType.VB))
            // player1ChoiceBox.items.addAll(this)
            // player2ChoiceBox.items.addAll(this)
        } else {
            player1ChoiceBox.items.add(ControllerType.NONE)
            player1ChoiceBox.items.add(ControllerType.STANDARD)

            player2ChoiceBox.items.add(ControllerType.NONE)
            player2ChoiceBox.items.add(ControllerType.STANDARD)
        }

        with(settings.controllerType(0)) {
            controllerTypePlayer1 = if (this !in player1ChoiceBox.items) ControllerType.NONE
            else this
        }

        with(settings.controllerType(1)) {
            controllerTypePlayer2 = if (this !in player2ChoiceBox.items) ControllerType.NONE
            else this
        }

        with(settings.controllerType(2)) {
            controllerTypePlayer3 = if (this !in player3ChoiceBox.items) ControllerType.NONE
            else this
        }

        with(settings.controllerType(3)) {
            controllerTypePlayer4 = if (this !in player4ChoiceBox.items) ControllerType.NONE
            else this
        }

        with(hasFourScore) {
            player3Box.isDisable = !this
            player4Box.isDisable = !this
        }
    }

    @FXML
    private fun openPlayerSettings(event: ActionEvent) {
        val player = ((event.source as Node).userData as String).toInt()

        fun ControllerType.create() = when (this) {
            ControllerType.STANDARD -> StandardControllerSettingsWindow(controllerKeys[player])
            ControllerType.ZAPPER -> ZapperSettingsWindow(zapperDetectionRadius, player)
            else -> null
        }

        val playerSettingsWindow = when (player) {
            1 -> controllerTypePlayer1.create()
            2 -> controllerTypePlayer2.create()
            3 -> controllerTypePlayer3.create()
            4 -> controllerTypePlayer4.create()
            else -> return
        }

        with(playerSettingsWindow ?: return) {
            beanFactory.autowireBean(this)
            beanFactory.initializeBean(this, "playerSettingsWindow")
            showAndWait(this@SettingsWindow)

            save(null)
        }
    }

    @FXML
    private fun openExpansionPortDeviceSettings(event: ActionEvent) {
        val expansionPortDeviceSettingsWindow = when (expansionPortDevice) {
            ZAPPER -> ZapperSettingsWindow(zapperDetectionRadius, ControlDevice.EXP_DEVICE_PORT)
            ASCII_TURBO_FILE -> AsciiTurboFileSettingsWindow(settings.asciiTurboFileSlot)
            else -> return
        }

        with(expansionPortDeviceSettingsWindow) {
            beanFactory.autowireBean(this)
            beanFactory.initializeBean(this, "expansionPortDeviceSettingsWindow")

            showAndWait(this@SettingsWindow)

            if (expansionPortDeviceSettingsWindow.saved) {
                if (expansionPortDeviceSettingsWindow is AsciiTurboFileSettingsWindow) {
                    settings.asciiTurboFileSlot = expansionPortDeviceSettingsWindow.slot
                }
            }

            save(null)
        }
    }

    @FXML
    private fun save(event: ActionEvent?) {
        settings.consoleType = consoleType

        settings.controllerType(0, controllerTypePlayer1)
        settings.controllerType(1, controllerTypePlayer2)

        settings.controllerKeys(0, controllerKeys[0])
        settings.controllerKeys(1, controllerKeys[1])

        settings.flag(EmulationFlag.HAS_FOUR_SCORE, hasFourScore)
        settings.flag(EmulationFlag.AUTO_CONFIGURE_INPUT, automaticallyConfigureControllersWhenLoadingGameCheckBox.isSelected)

        if (hasFourScore) {
            settings.controllerType(2, controllerTypePlayer3)
            settings.controllerType(3, controllerTypePlayer4)
            settings.controllerKeys(2, controllerKeys[2])
            settings.controllerKeys(3, controllerKeys[3])
        } else {
            settings.controllerType(2, ControllerType.NONE)
            settings.controllerType(3, ControllerType.NONE)
            settings.controllerKeys(2, KeyMapping())
            settings.controllerKeys(3, KeyMapping())
        }

        settings.expansionPortDevice = expansionPortDevice

        settings.flag(EmulationFlag.DISABLE_NOISE_MODE_FLAG, disableNoiseChannelModeFlagCheckBox.isSelected)
        settings.flag(EmulationFlag.SILENCE_TRIANGLE_HIGH_FREQ, muteUltrasonicFrequenciesOnTriangleChannelCheckBox.isSelected)
        settings.flag(EmulationFlag.SWAP_DUTY_CYCLES, swapSquareChannelsDutyCyclesCheckBox.isSelected)
        settings.flag(EmulationFlag.REDUCE_DMC_POPPING, reducePoppingSoundsOnTheDMCChannelCheckBox.isSelected)
        settings.sampleRate = sampleRateChoiceBox.value.toInt()

        settings.flag(EmulationFlag.INTEGER_FPS_MODE, enableIntegerFPSModeCheckBox.isSelected)
        settings.paletteType = paletteChoiceBox.value
        settings.flag(EmulationFlag.REMOVE_SPRITE_LIMIT, removeSpriteLimitCheckBox.isSelected)
        settings.flag(EmulationFlag.ADAPTIVE_SPRITE_LIMIT, autoReenableSpriteLimitAsNeededCheckBox.isSelected)
        settings.flag(EmulationFlag.FORCE_SPRITES_FIRST_COLUMN, forceSpriteDisplayInFirstColumnCheckBox.isSelected)
        settings.flag(EmulationFlag.FORCE_BACKGROUND_FIRST_COLUMN, forceBackgroundDisplayInFirstColumnCheckBox.isSelected)
        settings.flag(EmulationFlag.DISABLE_SPRITES, disableSpritesCheckBox.isSelected)
        settings.flag(EmulationFlag.DISABLE_BACKGROUND, disableBackgroundCheckBox.isSelected)

        settings.flag(EmulationFlag.ENABLE_PPU_OAM_ROW_CORRUPTION, enablePPUOAMRowCorruptionEmulationCheckBox.isSelected)
        settings.flag(EmulationFlag.ENABLE_PPU_2000_SCROLL_GLITCH, enablePPU200020052006FirstWriteScrollGlitchEmulationCheckBox.isSelected)
        settings.flag(EmulationFlag.ENABLE_PPU_2006_SCROLL_GLITCH, enablePPU2006WriteScrollGlitchEmulationCheckBox.isSelected)
        settings.flag(EmulationFlag.RANDOMIZE_CPU_PPU_ALIGNMENT, randomizePowerOnCPUPPUAlignmentCheckBox.isSelected)
        settings.flag(EmulationFlag.RANDOMIZE_MAPPER_POWER_ON_STATE, randomizePowerOnStateForMappersCheckBox.isSelected)
        settings.ramPowerOnState = defaultPowerOnStateForRAMChoiceBox.value
        settings.flag(EmulationFlag.ENABLE_OAM_DECAY, enableOAMRAMDecayCheckBox.isSelected)
        settings.flag(EmulationFlag.DISABLE_PALETTE_READ, disablePPUPaletteReadsCheckBox.isSelected)
        settings.flag(EmulationFlag.DISABLE_OAM_ADDR_BUG, disablePPUOAMADDRBugEmulationCheckBox.isSelected)
        settings.flag(EmulationFlag.DISABLE_PPU_RESET, doNotResetPPUWhenResettingConsoleCheckBox.isSelected)
        settings.flag(EmulationFlag.DISABLE_PPU_2004_READS, disablePPU2004ReadsCheckBox.isSelected)
        settings.flag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR, useNESHVC101BehaviourCheckBox.isSelected)
        settings.flag(EmulationFlag.MMC3_IRQ_ALT_BEHAVIOR, useAlternativeMMC3IRQBehaviourCheckBox.isSelected)
        settings.flag(EmulationFlag.ALLOW_INVALID_INPUT, allowInvalidInputCheckBox.isSelected)
        settings.extraScanlinesBeforeNmi = additionalScanlinesBeforeNMISpinner.value.toInt()
        settings.extraScanlinesAfterNmi = additionalScanlinesAfterNMISpinner.value.toInt()

        settings.flag(EmulationFlag.FDS_AUTO_LOAD_DISK, autoInsertDisk1SideAWhenStartingCheckBox.isSelected)
        settings.flag(EmulationFlag.FDS_AUTO_INSERT_DISK, autoSwitchDisksCheckBox.isSelected)

        zapperDetectionRadius.copyInto(settings.zapperDetectionRadius)

        preferences.save()

        saved = true

        if (event != null) close()
    }

    @FXML
    private fun cancel(event: ActionEvent?) {
        settings.restoreState(initialState)

        preferences.save()

        if (event != null) close()
    }

    @FXML
    private fun reset() {
        settings.reset()
        reset = true
        onStart()
    }
}
