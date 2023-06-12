package br.tiagohm.nestalgia.desktop.gui.settings

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.ControlDevice.Companion.EXP_DEVICE_PORT
import br.tiagohm.nestalgia.core.ControlDevice.Companion.PORT_COUNT
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.EmulationFlag.*
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
    @FXML private lateinit var port1ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var port2ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var subPort1ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var subPort2ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var subPort3ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var subPort4ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var expansionPortChoiceBox: ChoiceBox<ControllerType>

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
    @FXML private lateinit var useAlternativeMMC3IRQBehaviourCheckBox: CheckBox
    @FXML private lateinit var allowInvalidInputCheckBox: CheckBox
    @FXML private lateinit var additionalScanlinesBeforeNMISpinner: Spinner<Double>
    @FXML private lateinit var additionalScanlinesAfterNMISpinner: Spinner<Double>

    // FDS.
    @FXML private lateinit var autoInsertDisk1SideAWhenStartingCheckBox: CheckBox
    @FXML private lateinit var autoSwitchDisksCheckBox: CheckBox

    private val initialState = Snapshot()
    private val controllerKeys = Array(PORT_COUNT) { KeyMapping() }
    private val zapperDetectionRadius = IntArray(PORT_COUNT) { 1 }
    private var reset = false

    val settings
        get() = preferences.emulationSettings

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

        automaticallyConfigureControllersWhenLoadingGameCheckBox.isSelected = settings.flag(AUTO_CONFIGURE_INPUT)
        consoleTypeChoiceBox.value = settings.consoleType

        port1ChoiceBox.value = settings.controllerType(0)
        port2ChoiceBox.value = settings.controllerType(1)

        repeat(PORT_COUNT) { settings.controllerKeys(it).copyTo(controllerKeys[it]) }
        settings.zapperDetectionRadius.copyInto(zapperDetectionRadius)

        expansionPortChoiceBox.value = settings.expansionPortDevice

        sampleRateChoiceBox.value = settings.sampleRate.toString()
        disableNoiseChannelModeFlagCheckBox.isSelected = settings.flag(DISABLE_NOISE_MODE_FLAG)
        muteUltrasonicFrequenciesOnTriangleChannelCheckBox.isSelected = settings.flag(SILENCE_TRIANGLE_HIGH_FREQ)
        swapSquareChannelsDutyCyclesCheckBox.isSelected = settings.flag(SWAP_DUTY_CYCLES)
        reducePoppingSoundsOnTheDMCChannelCheckBox.isSelected = settings.flag(REDUCE_DMC_POPPING)

        enableIntegerFPSModeCheckBox.isSelected = settings.flag(INTEGER_FPS_MODE)
        paletteChoiceBox.value = settings.paletteType
        removeSpriteLimitCheckBox.isSelected = settings.flag(REMOVE_SPRITE_LIMIT)
        autoReenableSpriteLimitAsNeededCheckBox.isSelected = settings.flag(ADAPTIVE_SPRITE_LIMIT)
        autoReenableSpriteLimitAsNeededCheckBox.disableProperty().bind(!removeSpriteLimitCheckBox.selectedProperty())
        forceSpriteDisplayInFirstColumnCheckBox.isSelected = settings.flag(FORCE_SPRITES_FIRST_COLUMN)
        forceBackgroundDisplayInFirstColumnCheckBox.isSelected = settings.flag(FORCE_BACKGROUND_FIRST_COLUMN)
        disableSpritesCheckBox.isSelected = settings.flag(DISABLE_SPRITES)
        disableBackgroundCheckBox.isSelected = settings.flag(DISABLE_BACKGROUND)

        enablePPUOAMRowCorruptionEmulationCheckBox.isSelected = settings.flag(ENABLE_PPU_OAM_ROW_CORRUPTION)
        enablePPU200020052006FirstWriteScrollGlitchEmulationCheckBox.isSelected = settings.flag(ENABLE_PPU_2000_SCROLL_GLITCH)
        enablePPU2006WriteScrollGlitchEmulationCheckBox.isSelected = settings.flag(ENABLE_PPU_2006_SCROLL_GLITCH)
        randomizePowerOnCPUPPUAlignmentCheckBox.isSelected = settings.flag(RANDOMIZE_CPU_PPU_ALIGNMENT)
        randomizePowerOnStateForMappersCheckBox.isSelected = settings.flag(RANDOMIZE_MAPPER_POWER_ON_STATE)
        defaultPowerOnStateForRAMChoiceBox.value = settings.ramPowerOnState
        enableOAMRAMDecayCheckBox.isSelected = settings.flag(ENABLE_OAM_DECAY)
        disablePPUPaletteReadsCheckBox.isSelected = settings.flag(DISABLE_PALETTE_READ)
        disablePPUOAMADDRBugEmulationCheckBox.isSelected = settings.flag(DISABLE_OAM_ADDR_BUG)
        doNotResetPPUWhenResettingConsoleCheckBox.isSelected = settings.flag(DISABLE_PPU_RESET)
        disablePPU2004ReadsCheckBox.isSelected = settings.flag(DISABLE_PPU_2004_READS)
        useAlternativeMMC3IRQBehaviourCheckBox.isSelected = settings.flag(MMC3_IRQ_ALT_BEHAVIOR)
        allowInvalidInputCheckBox.isSelected = settings.flag(ALLOW_INVALID_INPUT)
        additionalScanlinesBeforeNMISpinner.valueFactory.value = settings.extraScanlinesBeforeNmi.toDouble()
        additionalScanlinesAfterNMISpinner.valueFactory.value = settings.extraScanlinesAfterNmi.toDouble()

        autoInsertDisk1SideAWhenStartingCheckBox.isSelected = settings.flag(FDS_AUTO_LOAD_DISK)
        autoSwitchDisksCheckBox.isSelected = settings.flag(FDS_AUTO_INSERT_DISK)

        expansionPortChoiceBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updatePortOptions()
        }

        updatePortOptions()
    }

    override fun onStop() {
        if (!saved) {
            cancel(null)
        }
    }

    private fun updatePortOptions() {

    }

    @FXML
    private fun openPortSettings(event: ActionEvent) {
        val port = ((event.source as Node).userData as String).toInt()

        fun ControllerType.create() = when (this) {
            NES_CONTROLLER,
            FAMICOM_CONTROLLER -> StandardControllerSettingsWindow(controllerKeys[port])
            NES_ZAPPER,
            FAMICOM_ZAPPER -> ZapperSettingsWindow(zapperDetectionRadius, port)
            else -> null
        }

        val portSettingsWindow = when (port) {
            1 -> port1ChoiceBox.value.create()
            2 -> port2ChoiceBox.value.create()
            else -> return
        }

        with(portSettingsWindow ?: return) {
            beanFactory.autowireBean(this)
            beanFactory.initializeBean(this, "portSettingsWindow")
            showAndWait(this@SettingsWindow)

            save(null)
        }
    }

    @FXML
    private fun openSubPortSettings(event: ActionEvent) {

    }

    @FXML
    private fun openExpansionPortSettings(event: ActionEvent) {
        val expansionPortSettingsWindow = when (expansionPortChoiceBox.value) {
            FAMICOM_ZAPPER -> ZapperSettingsWindow(zapperDetectionRadius, EXP_DEVICE_PORT)
            ASCII_TURBO_FILE -> AsciiTurboFileSettingsWindow(settings.asciiTurboFileSlot)
            else -> return
        }

        with(expansionPortSettingsWindow) {
            beanFactory.autowireBean(this)
            beanFactory.initializeBean(this, "expansionPortSettingsWindow")

            showAndWait(this@SettingsWindow)

            if (expansionPortSettingsWindow.saved) {
                if (expansionPortSettingsWindow is AsciiTurboFileSettingsWindow) {
                    settings.asciiTurboFileSlot = expansionPortSettingsWindow.slot
                }
            }

            save(null)
        }
    }

    @FXML
    private fun save(event: ActionEvent?) {
        settings.consoleType = consoleTypeChoiceBox.value

        settings.controllerType(0, port1ChoiceBox.value)
        settings.controllerType(1, port2ChoiceBox.value)

        repeat(PORT_COUNT) { settings.controllerKeys(it, controllerKeys[it]) }

        settings.flag(AUTO_CONFIGURE_INPUT, automaticallyConfigureControllersWhenLoadingGameCheckBox.isSelected)

        // TODO: FOUR-SCORE SUB PORTS!

        settings.expansionPortDevice = expansionPortChoiceBox.value

        settings.flag(DISABLE_NOISE_MODE_FLAG, disableNoiseChannelModeFlagCheckBox.isSelected)
        settings.flag(SILENCE_TRIANGLE_HIGH_FREQ, muteUltrasonicFrequenciesOnTriangleChannelCheckBox.isSelected)
        settings.flag(SWAP_DUTY_CYCLES, swapSquareChannelsDutyCyclesCheckBox.isSelected)
        settings.flag(REDUCE_DMC_POPPING, reducePoppingSoundsOnTheDMCChannelCheckBox.isSelected)
        settings.sampleRate = sampleRateChoiceBox.value.toInt()

        settings.flag(INTEGER_FPS_MODE, enableIntegerFPSModeCheckBox.isSelected)
        settings.paletteType = paletteChoiceBox.value
        settings.flag(REMOVE_SPRITE_LIMIT, removeSpriteLimitCheckBox.isSelected)
        settings.flag(ADAPTIVE_SPRITE_LIMIT, autoReenableSpriteLimitAsNeededCheckBox.isSelected)
        settings.flag(FORCE_SPRITES_FIRST_COLUMN, forceSpriteDisplayInFirstColumnCheckBox.isSelected)
        settings.flag(FORCE_BACKGROUND_FIRST_COLUMN, forceBackgroundDisplayInFirstColumnCheckBox.isSelected)
        settings.flag(DISABLE_SPRITES, disableSpritesCheckBox.isSelected)
        settings.flag(DISABLE_BACKGROUND, disableBackgroundCheckBox.isSelected)

        settings.flag(ENABLE_PPU_OAM_ROW_CORRUPTION, enablePPUOAMRowCorruptionEmulationCheckBox.isSelected)
        settings.flag(ENABLE_PPU_2000_SCROLL_GLITCH, enablePPU200020052006FirstWriteScrollGlitchEmulationCheckBox.isSelected)
        settings.flag(ENABLE_PPU_2006_SCROLL_GLITCH, enablePPU2006WriteScrollGlitchEmulationCheckBox.isSelected)
        settings.flag(RANDOMIZE_CPU_PPU_ALIGNMENT, randomizePowerOnCPUPPUAlignmentCheckBox.isSelected)
        settings.flag(RANDOMIZE_MAPPER_POWER_ON_STATE, randomizePowerOnStateForMappersCheckBox.isSelected)
        settings.ramPowerOnState = defaultPowerOnStateForRAMChoiceBox.value
        settings.flag(ENABLE_OAM_DECAY, enableOAMRAMDecayCheckBox.isSelected)
        settings.flag(DISABLE_PALETTE_READ, disablePPUPaletteReadsCheckBox.isSelected)
        settings.flag(DISABLE_OAM_ADDR_BUG, disablePPUOAMADDRBugEmulationCheckBox.isSelected)
        settings.flag(DISABLE_PPU_RESET, doNotResetPPUWhenResettingConsoleCheckBox.isSelected)
        settings.flag(DISABLE_PPU_2004_READS, disablePPU2004ReadsCheckBox.isSelected)
        settings.flag(MMC3_IRQ_ALT_BEHAVIOR, useAlternativeMMC3IRQBehaviourCheckBox.isSelected)
        settings.flag(ALLOW_INVALID_INPUT, allowInvalidInputCheckBox.isSelected)
        settings.extraScanlinesBeforeNmi = additionalScanlinesBeforeNMISpinner.value.toInt()
        settings.extraScanlinesAfterNmi = additionalScanlinesAfterNMISpinner.value.toInt()

        settings.flag(FDS_AUTO_LOAD_DISK, autoInsertDisk1SideAWhenStartingCheckBox.isSelected)
        settings.flag(FDS_AUTO_INSERT_DISK, autoSwitchDisksCheckBox.isSelected)

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
