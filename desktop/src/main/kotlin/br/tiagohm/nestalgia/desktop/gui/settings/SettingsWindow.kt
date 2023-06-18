package br.tiagohm.nestalgia.desktop.gui.settings

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.ControlDevice.Companion.EXP_DEVICE_PORT
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.EmulationFlag.*
import br.tiagohm.nestalgia.desktop.app.Preferences
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.converters.ConsoleTypeStringConverter
import br.tiagohm.nestalgia.desktop.gui.converters.ControllerTypeStringConverter
import br.tiagohm.nestalgia.desktop.gui.converters.RamPowerOnStateStringConverter
import br.tiagohm.nestalgia.desktop.gui.settings.controllers.ArkanoidSettingsWindow
import br.tiagohm.nestalgia.desktop.gui.settings.controllers.StandardControllerSettingsWindow
import br.tiagohm.nestalgia.desktop.gui.settings.controllers.ZapperSettingsWindow
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Spinner
import javafx.scene.layout.Pane
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.stereotype.Component

@Component
class SettingsWindow : AbstractWindow() {

    override val resourceName = "Settings"

    @Autowired private lateinit var preferences: Preferences
    @Autowired private lateinit var console: Console
    @Autowired private lateinit var globalSettings: EmulationSettings
    @Autowired private lateinit var consoleSettings: EmulationSettings
    @Autowired private lateinit var beanFactory: AutowireCapableBeanFactory

    @FXML private lateinit var profileChoiceBox: ChoiceBox<String>
    @FXML private lateinit var fourScorePane: Pane
    @FXML private lateinit var twoFourPlayerAdapterPane: Pane
    @FXML private lateinit var resetToGlobalButton: Button

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
    @FXML private lateinit var expansionSubPort1ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var expansionSubPort2ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var expansionSubPort3ChoiceBox: ChoiceBox<ControllerType>
    @FXML private lateinit var expansionSubPort4ChoiceBox: ChoiceBox<ControllerType>

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

    val settings
        get() = if (profileChoiceBox.value == "CONSOLE") consoleSettings else globalSettings

    override fun onCreate() {
        title = "Settings"
        resizable = false

        profileChoiceBox.valueProperty().addListener { _, prev, _ ->
            if (prev == "GLOBAL") globalSettings.save()
            else if (prev == "CONSOLE") consoleSettings.save()

            settings.load()
        }

        consoleTypeChoiceBox.converter = ConsoleTypeStringConverter
        port1ChoiceBox.converter = ControllerTypeStringConverter
        port1ChoiceBox.converter = ControllerTypeStringConverter
        port2ChoiceBox.converter = ControllerTypeStringConverter
        subPort1ChoiceBox.converter = ControllerTypeStringConverter
        subPort2ChoiceBox.converter = ControllerTypeStringConverter
        subPort3ChoiceBox.converter = ControllerTypeStringConverter
        subPort4ChoiceBox.converter = ControllerTypeStringConverter
        expansionPortChoiceBox.converter = ControllerTypeStringConverter
        expansionSubPort1ChoiceBox.converter = ControllerTypeStringConverter
        expansionSubPort2ChoiceBox.converter = ControllerTypeStringConverter
        expansionSubPort3ChoiceBox.converter = ControllerTypeStringConverter
        expansionSubPort4ChoiceBox.converter = ControllerTypeStringConverter
        defaultPowerOnStateForRAMChoiceBox.converter = RamPowerOnStateStringConverter

        expansionPortChoiceBox.selectionModel.selectedItemProperty().addListener { _, _, _ -> updatePortOptions() }
        port1ChoiceBox.selectionModel.selectedItemProperty().addListener { _, _, _ -> updatePortOptions() }

        resetToGlobalButton.disableProperty().bind(profileChoiceBox.valueProperty().isEqualTo("GLOBAL"))
    }

    override fun onStart() {
        super.onStart()

        if (console.isRunning) {
            profileChoiceBox.items.setAll("GLOBAL", "CONSOLE")
            profileChoiceBox.value = "CONSOLE"
        } else {
            profileChoiceBox.items.setAll("GLOBAL")
            profileChoiceBox.value = "GLOBAL"
        }

        settings.load()
    }

    override fun onStop() {
        settings.save()
    }

    private fun EmulationSettings.save() {
        consoleType = consoleTypeChoiceBox.value

        port1.type = port1ChoiceBox.value
        port2.type = port2ChoiceBox.value

        subPort1[0].type = subPort1ChoiceBox.value
        subPort1[1].type = subPort2ChoiceBox.value
        subPort1[2].type = subPort3ChoiceBox.value
        subPort1[3].type = subPort4ChoiceBox.value

        expansionPort.type = expansionPortChoiceBox.value

        expansionSubPort[0].type = expansionSubPort1ChoiceBox.value
        expansionSubPort[1].type = expansionSubPort2ChoiceBox.value
        expansionSubPort[2].type = expansionSubPort3ChoiceBox.value
        expansionSubPort[3].type = expansionSubPort4ChoiceBox.value

        flag(AUTO_CONFIGURE_INPUT, automaticallyConfigureControllersWhenLoadingGameCheckBox.isSelected)

        flag(DISABLE_NOISE_MODE_FLAG, disableNoiseChannelModeFlagCheckBox.isSelected)
        flag(SILENCE_TRIANGLE_HIGH_FREQ, muteUltrasonicFrequenciesOnTriangleChannelCheckBox.isSelected)
        flag(SWAP_DUTY_CYCLES, swapSquareChannelsDutyCyclesCheckBox.isSelected)
        flag(REDUCE_DMC_POPPING, reducePoppingSoundsOnTheDMCChannelCheckBox.isSelected)
        sampleRate = sampleRateChoiceBox.value.toInt()

        flag(INTEGER_FPS_MODE, enableIntegerFPSModeCheckBox.isSelected)
        paletteType = paletteChoiceBox.value
        flag(REMOVE_SPRITE_LIMIT, removeSpriteLimitCheckBox.isSelected)
        flag(ADAPTIVE_SPRITE_LIMIT, autoReenableSpriteLimitAsNeededCheckBox.isSelected)
        flag(FORCE_SPRITES_FIRST_COLUMN, forceSpriteDisplayInFirstColumnCheckBox.isSelected)
        flag(FORCE_BACKGROUND_FIRST_COLUMN, forceBackgroundDisplayInFirstColumnCheckBox.isSelected)
        flag(DISABLE_SPRITES, disableSpritesCheckBox.isSelected)
        flag(DISABLE_BACKGROUND, disableBackgroundCheckBox.isSelected)

        flag(ENABLE_PPU_OAM_ROW_CORRUPTION, enablePPUOAMRowCorruptionEmulationCheckBox.isSelected)
        flag(ENABLE_PPU_2000_SCROLL_GLITCH, enablePPU200020052006FirstWriteScrollGlitchEmulationCheckBox.isSelected)
        flag(ENABLE_PPU_2006_SCROLL_GLITCH, enablePPU2006WriteScrollGlitchEmulationCheckBox.isSelected)
        flag(RANDOMIZE_CPU_PPU_ALIGNMENT, randomizePowerOnCPUPPUAlignmentCheckBox.isSelected)
        flag(RANDOMIZE_MAPPER_POWER_ON_STATE, randomizePowerOnStateForMappersCheckBox.isSelected)
        ramPowerOnState = defaultPowerOnStateForRAMChoiceBox.value
        flag(ENABLE_OAM_DECAY, enableOAMRAMDecayCheckBox.isSelected)
        flag(DISABLE_PALETTE_READ, disablePPUPaletteReadsCheckBox.isSelected)
        flag(DISABLE_OAM_ADDR_BUG, disablePPUOAMADDRBugEmulationCheckBox.isSelected)
        flag(DISABLE_PPU_RESET, doNotResetPPUWhenResettingConsoleCheckBox.isSelected)
        flag(DISABLE_PPU_2004_READS, disablePPU2004ReadsCheckBox.isSelected)
        flag(MMC3_IRQ_ALT_BEHAVIOR, useAlternativeMMC3IRQBehaviourCheckBox.isSelected)
        flag(ALLOW_INVALID_INPUT, allowInvalidInputCheckBox.isSelected)
        extraScanlinesBeforeNmi = additionalScanlinesBeforeNMISpinner.value.toInt()
        extraScanlinesAfterNmi = additionalScanlinesAfterNMISpinner.value.toInt()

        flag(FDS_AUTO_LOAD_DISK, autoInsertDisk1SideAWhenStartingCheckBox.isSelected)
        flag(FDS_AUTO_INSERT_DISK, autoSwitchDisksCheckBox.isSelected)

        if (this === globalSettings) {
            preferences.save()
        }

        markAsNeedControllerUpdate()
    }

    private fun EmulationSettings.load() {
        automaticallyConfigureControllersWhenLoadingGameCheckBox.isSelected = flag(AUTO_CONFIGURE_INPUT)
        consoleTypeChoiceBox.value = consoleType

        port1ChoiceBox.value = port1.type
        port2ChoiceBox.value = port2.type

        subPort1ChoiceBox.value = subPort1[0].type
        subPort2ChoiceBox.value = subPort1[1].type
        subPort3ChoiceBox.value = subPort1[2].type
        subPort4ChoiceBox.value = subPort1[3].type

        expansionPortChoiceBox.value = expansionPort.type

        expansionSubPort1ChoiceBox.value = expansionSubPort[0].type
        expansionSubPort2ChoiceBox.value = expansionSubPort[1].type
        expansionSubPort3ChoiceBox.value = expansionSubPort[2].type
        expansionSubPort4ChoiceBox.value = expansionSubPort[3].type

        sampleRateChoiceBox.value = sampleRate.toString()
        disableNoiseChannelModeFlagCheckBox.isSelected = flag(DISABLE_NOISE_MODE_FLAG)
        muteUltrasonicFrequenciesOnTriangleChannelCheckBox.isSelected = flag(SILENCE_TRIANGLE_HIGH_FREQ)
        swapSquareChannelsDutyCyclesCheckBox.isSelected = flag(SWAP_DUTY_CYCLES)
        reducePoppingSoundsOnTheDMCChannelCheckBox.isSelected = flag(REDUCE_DMC_POPPING)

        enableIntegerFPSModeCheckBox.isSelected = flag(INTEGER_FPS_MODE)
        paletteChoiceBox.value = paletteType
        removeSpriteLimitCheckBox.isSelected = flag(REMOVE_SPRITE_LIMIT)
        autoReenableSpriteLimitAsNeededCheckBox.isSelected = flag(ADAPTIVE_SPRITE_LIMIT)
        autoReenableSpriteLimitAsNeededCheckBox.disableProperty().bind(!removeSpriteLimitCheckBox.selectedProperty())
        forceSpriteDisplayInFirstColumnCheckBox.isSelected = flag(FORCE_SPRITES_FIRST_COLUMN)
        forceBackgroundDisplayInFirstColumnCheckBox.isSelected = flag(FORCE_BACKGROUND_FIRST_COLUMN)
        disableSpritesCheckBox.isSelected = flag(DISABLE_SPRITES)
        disableBackgroundCheckBox.isSelected = flag(DISABLE_BACKGROUND)

        enablePPUOAMRowCorruptionEmulationCheckBox.isSelected = flag(ENABLE_PPU_OAM_ROW_CORRUPTION)
        enablePPU200020052006FirstWriteScrollGlitchEmulationCheckBox.isSelected = flag(ENABLE_PPU_2000_SCROLL_GLITCH)
        enablePPU2006WriteScrollGlitchEmulationCheckBox.isSelected = flag(ENABLE_PPU_2006_SCROLL_GLITCH)
        randomizePowerOnCPUPPUAlignmentCheckBox.isSelected = flag(RANDOMIZE_CPU_PPU_ALIGNMENT)
        randomizePowerOnStateForMappersCheckBox.isSelected = flag(RANDOMIZE_MAPPER_POWER_ON_STATE)
        defaultPowerOnStateForRAMChoiceBox.value = ramPowerOnState
        enableOAMRAMDecayCheckBox.isSelected = flag(ENABLE_OAM_DECAY)
        disablePPUPaletteReadsCheckBox.isSelected = flag(DISABLE_PALETTE_READ)
        disablePPUOAMADDRBugEmulationCheckBox.isSelected = flag(DISABLE_OAM_ADDR_BUG)
        doNotResetPPUWhenResettingConsoleCheckBox.isSelected = flag(DISABLE_PPU_RESET)
        disablePPU2004ReadsCheckBox.isSelected = flag(DISABLE_PPU_2004_READS)
        useAlternativeMMC3IRQBehaviourCheckBox.isSelected = flag(MMC3_IRQ_ALT_BEHAVIOR)
        allowInvalidInputCheckBox.isSelected = flag(ALLOW_INVALID_INPUT)
        additionalScanlinesBeforeNMISpinner.valueFactory.value = extraScanlinesBeforeNmi.toDouble()
        additionalScanlinesAfterNMISpinner.valueFactory.value = extraScanlinesAfterNmi.toDouble()

        autoInsertDisk1SideAWhenStartingCheckBox.isSelected = flag(FDS_AUTO_LOAD_DISK)
        autoSwitchDisksCheckBox.isSelected = flag(FDS_AUTO_INSERT_DISK)

        updatePortOptions()
    }

    private fun updatePortOptions() {
        when (expansionPortChoiceBox.value) {
            FOUR_PLAYER_ADAPTER -> {
                twoFourPlayerAdapterPane.isVisible = true
                twoFourPlayerAdapterPane.isManaged = true

                expansionSubPort1ChoiceBox.items.removeAll(TWO_PLAYER_ADAPTER_CONTROLLER_TYPES)
                expansionSubPort2ChoiceBox.items.removeAll(TWO_PLAYER_ADAPTER_CONTROLLER_TYPES)
                expansionSubPort3ChoiceBox.items.removeAll(TWO_PLAYER_ADAPTER_CONTROLLER_TYPES)
                expansionSubPort4ChoiceBox.items.removeAll(TWO_PLAYER_ADAPTER_CONTROLLER_TYPES)

                if (expansionSubPort1ChoiceBox.value !in expansionSubPort1ChoiceBox.items) {
                    expansionSubPort1ChoiceBox.value = NONE
                }
                if (expansionSubPort2ChoiceBox.value !in expansionSubPort2ChoiceBox.items) {
                    expansionSubPort2ChoiceBox.value = NONE
                }
                if (expansionSubPort3ChoiceBox.value !in expansionSubPort3ChoiceBox.items) {
                    expansionSubPort3ChoiceBox.value = NONE
                }
                if (expansionSubPort4ChoiceBox.value !in expansionSubPort4ChoiceBox.items) {
                    expansionSubPort4ChoiceBox.value = NONE
                }
            }
            TWO_PLAYER_ADAPTER -> {
                twoFourPlayerAdapterPane.isVisible = true
                twoFourPlayerAdapterPane.isManaged = true

                expansionSubPort1ChoiceBox.items.addAll(TWO_PLAYER_ADAPTER_CONTROLLER_TYPES)
                expansionSubPort2ChoiceBox.items.addAll(TWO_PLAYER_ADAPTER_CONTROLLER_TYPES)
                expansionSubPort3ChoiceBox.items.addAll(TWO_PLAYER_ADAPTER_CONTROLLER_TYPES)
                expansionSubPort4ChoiceBox.items.addAll(TWO_PLAYER_ADAPTER_CONTROLLER_TYPES)
            }
            else -> {
                twoFourPlayerAdapterPane.isVisible = false
                twoFourPlayerAdapterPane.isManaged = false
            }
        }

        fourScorePane.isVisible = port1ChoiceBox.value == FOUR_SCORE
        fourScorePane.isManaged = port1ChoiceBox.value == FOUR_SCORE
        port2ChoiceBox.parent.isDisable = port1ChoiceBox.value == FOUR_SCORE

        if (port1ChoiceBox.value == FOUR_SCORE) {
            port2ChoiceBox.value = NONE
        }
    }

    private fun ChoiceBox<ControllerType>.openPortSettings(port: Int, subPort: Int = 0) {
        val keyMapping = when (port) {
            0 -> settings.port1.keyMapping
            1 -> settings.port2.keyMapping
            EXP_DEVICE_PORT -> settings.expansionPort.keyMapping
            -1 -> settings.subPort1[subPort - 1].keyMapping
            -EXP_DEVICE_PORT -> settings.expansionSubPort[subPort - 1].keyMapping
            else -> return
        }

        val window = when (value) {
            NES_CONTROLLER,
            FAMICOM_CONTROLLER -> StandardControllerSettingsWindow(keyMapping)
            NES_ZAPPER,
            FAMICOM_ZAPPER -> ZapperSettingsWindow(settings.zapperDetectionRadius, keyMapping, port)
            NES_ARKANOID_CONTROLLER,
            FAMICOM_ARKANOID_CONTROLLER -> ArkanoidSettingsWindow(settings.arkanoidSensibility, keyMapping, port)
            else -> return
        }

        with(window) {
            beanFactory.autowireBean(this)
            beanFactory.initializeBean(this, "portSettingsWindow")
            showAndWait(this@SettingsWindow)
        }
    }

    @FXML
    private fun openPortSettings(event: ActionEvent) {
        when (((event.source as Node).userData as String).toInt()) {
            1 -> port1ChoiceBox.openPortSettings(0)
            2 -> port2ChoiceBox.openPortSettings(1)
        }
    }

    @FXML
    private fun openSubPortSettings(event: ActionEvent) {
        when (((event.source as Node).userData as String).toInt()) {
            1 -> subPort1ChoiceBox.openPortSettings(-1, 0)
            2 -> subPort2ChoiceBox.openPortSettings(-1, 1)
            3 -> subPort3ChoiceBox.openPortSettings(-1, 2)
            4 -> subPort4ChoiceBox.openPortSettings(-1, 3)
        }
    }

    @FXML
    private fun openExpansionPortSettings(event: ActionEvent) {
        expansionPortChoiceBox.openPortSettings(EXP_DEVICE_PORT)
    }

    @FXML
    private fun openExpansionSubPortSettings(event: ActionEvent) {
        when (((event.source as Node).userData as String).toInt()) {
            1 -> expansionSubPort1ChoiceBox.openPortSettings(-EXP_DEVICE_PORT, 0)
            2 -> expansionSubPort2ChoiceBox.openPortSettings(-EXP_DEVICE_PORT, 1)
            3 -> expansionSubPort3ChoiceBox.openPortSettings(-EXP_DEVICE_PORT, 2)
            4 -> expansionSubPort4ChoiceBox.openPortSettings(-EXP_DEVICE_PORT, 3)
        }
    }

    @FXML
    private fun resetToGlobal() {
        if (settings === consoleSettings) {
            globalSettings.copyTo(consoleSettings)
            settings.load()
        }
    }

    @FXML
    private fun resetToDefault() {
        settings.reset()
        settings.load()
    }

    companion object {

        @JvmStatic private val TWO_PLAYER_ADAPTER_CONTROLLER_TYPES = listOf(
            PACHINKO, SNES_MOUSE, SUBOR_MOUSE, VIRTUAL_BOY_CONTROLLER,
        )
    }
}
