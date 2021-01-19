package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.ui.*
import java.util.*
import javax.swing.JPanel

@ExperimentalUnsignedTypes
class ControllerConfig(
    private val emulator: Emulator,
    private val onSave: () -> Unit,
) : Dialog("Controller Settings") {

    private val settings = emulator.settings
    private var consoleType = settings.consoleType
    private val controllerTypes = Array(4) { settings.getControllerType(it) }
    private val controllerKeys = Array(4) { settings.getControllerKeys(it) }
    private var expansionDevice = settings.expansionDevice
    private var hasFourScore = settings.checkFlag(EmulationFlag.HAS_FOUR_SCORE)
    private var zapperDetectionRadius = settings.zapperDetectionRadius.copyOf()
    private var autoConfigureInput = settings.checkFlag(EmulationFlag.AUTO_CONFIGURE_INPUT)

    val isFourScoreAttached: Boolean
        get() {
            val isNes = consoleType == ConsoleType.NES
            return (isNes && hasFourScore) || (!isNes && expansionDevice == ExpansionPortDevice.FOUR_PLAYER_ADAPTER)
        }

    override val body: JPanel
        get() {
            val isNes = consoleType == ConsoleType.NES
            val isOriginalFamicom = !isNes && !settings.checkFlag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)

            val controllerTypes12 = ArrayList<ControllerType>()
            val controllerTypes34 = ArrayList<ControllerType>()

            controllerTypes34.add(ControllerType.NONE)
            controllerTypes34.add(ControllerType.STANDARD)
            // controllerTypes34.add(ControllerType.SNES_MOUSE)
            // controllerTypes34.add(ControllerType.SUBOR_MOUSE)

            if (!isNes) {
                // controllerTypes34.add(ControllerType.SNES)
            }

            if (isOriginalFamicom) {
                controllerTypes12.add(ControllerType.STANDARD)
            } else if (isNes && !hasFourScore) {
                controllerTypes12.addAll(controllerTypes34)
                // controllerTypes12.add(ControllerType.ARKANOID)
                // controllerTypes12.add(ControllerType.POWER_PAD)
                controllerTypes12.add(ControllerType.ZAPPER)
                // controllerTypes12.add(ControllerType.SNES)
                // controllerTypes12.add(ControllerType.VB)
            } else {
                controllerTypes12.addAll(controllerTypes34)
            }

            if (!controllerTypes12.contains(controllerTypes[0])) controllerTypes[0] = ControllerType.NONE
            if (!controllerTypes12.contains(controllerTypes[1])) controllerTypes[1] = ControllerType.NONE
            if (!controllerTypes34.contains(controllerTypes[2])) controllerTypes[2] = ControllerType.NONE
            if (!controllerTypes34.contains(controllerTypes[3])) controllerTypes[3] = ControllerType.NONE

            return panel(9, 3, margin = margin(16)) {
                var row = 0

                checkbox(
                    row, 0,
                    autoConfigureInput,
                    "Automatically configure controllers when loading a game",
                    onChanged = {
                        autoConfigureInput = it
                    })

                row++

                label("Controller type:", row, 0)
                dropdown(
                    row, 1,
                    CONSOLE_TYPES,
                    consoleType,
                    fill = Fill.HORIZONTAL,
                    onChanged = {
                        consoleType = it
                        updateView()
                    },
                )

                row++

                label("Player 1:", row, 0)
                dropdown(
                    row, 1,
                    controllerTypes12,
                    controllerTypes[0],
                    onChanged = {
                        controllerTypes[0] = it
                        updateView()
                    })
                button(
                    "Settings", row, 2,
                    isEnabled = controllerTypes[0] != ControllerType.NONE,
                    fill = Fill.HORIZONTAL,
                    onClick = {
                        when (controllerTypes[0]) {
                            ControllerType.STANDARD -> showStandardControllerConfig(0)
                            ControllerType.ZAPPER -> showZapperConfig(0)
                            else -> {
                            }
                        }
                    }
                )

                row++

                label("Player 2:", row, 0)
                dropdown(
                    row, 1,
                    controllerTypes12,
                    controllerTypes[1],
                    onChanged = {
                        controllerTypes[1] = it
                        updateView()
                    }
                )
                button(
                    "Settings", row, 2,
                    isEnabled = controllerTypes[1] != ControllerType.NONE,
                    fill = Fill.HORIZONTAL,
                    onClick = {
                        when (controllerTypes[1]) {
                            ControllerType.STANDARD -> showStandardControllerConfig(1)
                            ControllerType.ZAPPER -> showZapperConfig(1)
                            else -> {
                            }
                        }
                    }
                )

                if (isNes) {
                    row++

                    checkbox(row, 0, hasFourScore, "Four Score", colSpan = 2, onChanged = {
                        hasFourScore = it
                        updateView()
                    })
                }

                if (isFourScoreAttached) {
                    row++

                    label("Player 3:", row, 0)
                    dropdown(
                        row, 1,
                        controllerTypes34,
                        controllerTypes[2],
                        onChanged = {
                            controllerTypes[2] = it
                            updateView()
                        })
                    button(
                        "Settings",
                        row, 2,
                        isEnabled = controllerTypes[2] != ControllerType.NONE,
                        fill = Fill.HORIZONTAL,
                        onClick = {
                            when (controllerTypes[2]) {
                                ControllerType.STANDARD -> showStandardControllerConfig(2)
                                else -> {
                                }
                            }
                        }
                    )

                    row++

                    label("Player 4:", row, 0)
                    dropdown(
                        row, 1,
                        controllerTypes34,
                        controllerTypes[3],
                        onChanged = {
                            controllerTypes[3] = it
                            updateView()
                        })
                    button(
                        "Settings",
                        row, 2,
                        isEnabled = controllerTypes[3] != ControllerType.NONE,
                        fill = Fill.HORIZONTAL,
                        onClick = {
                            when (controllerTypes[3]) {
                                ControllerType.STANDARD -> showStandardControllerConfig(3)
                                else -> {
                                }
                            }
                        }
                    )
                }

                if (!isNes) {
                    row++

                    label("Expansion Port:", row, 0)
                    dropdown(
                        row, 1,
                        EXPANSION_DEVICES,
                        expansionDevice,
                        onChanged = {
                            expansionDevice = it
                            updateView()
                        }
                    )
                    button(
                        "Settings",
                        row, 2,
                        isEnabled = expansionDevice == ExpansionPortDevice.ZAPPER,
                        fill = Fill.HORIZONTAL,
                        onClick = {
                            when (expansionDevice) {
                                ExpansionPortDevice.ZAPPER -> showZapperConfig(ControlDevice.EXP_DEVICE_PORT)
                                else -> {
                                }
                            }
                        }
                    )
                }

                button(
                    "Save",
                    ++row, 0,
                    colSpan = 3,
                    fill = Fill.NONE,
                    anchor = Anchor.CENTER,
                    minimumSize = Size(100),
                    onClick = ::onOk
                )
            }
        }

    private fun showStandardControllerConfig(port: Int) {
        StandardControllerConfig.show(port, emulator.keyManager, controllerKeys[port]) {
            controllerKeys[port] = it
        }
    }

    private fun showZapperConfig(port: Int) {
        ZapperConfig.show(port, zapperDetectionRadius[port]) {
            zapperDetectionRadius[port] = it
        }
    }

    private fun onOk() {
        val isNes = consoleType == ConsoleType.NES
        settings.consoleType = consoleType

        settings.setControllerType(0, controllerTypes[0])
        settings.setControllerType(1, controllerTypes[1])

        settings.setControllerKeys(0, controllerKeys[0])
        settings.setControllerKeys(1, controllerKeys[1])

        val hasFourScore = isFourScoreAttached

        if (hasFourScore) settings.setFlag(EmulationFlag.HAS_FOUR_SCORE)
        else settings.clearFlag(EmulationFlag.HAS_FOUR_SCORE)

        if (autoConfigureInput) settings.setFlag(EmulationFlag.AUTO_CONFIGURE_INPUT)
        else settings.clearFlag(EmulationFlag.AUTO_CONFIGURE_INPUT)

        if (hasFourScore) {
            settings.setControllerType(2, controllerTypes[2])
            settings.setControllerType(3, controllerTypes[3])
            settings.setControllerKeys(2, controllerKeys[2])
            settings.setControllerKeys(3, controllerKeys[3])
        } else {
            settings.setControllerType(2, ControllerType.NONE)
            settings.setControllerType(3, ControllerType.NONE)
            settings.setControllerKeys(2, KeyMapping.NONE)
            settings.setControllerKeys(3, KeyMapping.NONE)
        }

        if (!isNes) {
            settings.expansionDevice = expansionDevice
        }

        zapperDetectionRadius.copyInto(settings.zapperDetectionRadius)

        onSave()

        dispose()
    }

    companion object {
        private val CONSOLE_TYPES = listOf(*ConsoleType.values())

        private val EXPANSION_DEVICES = listOf(
            ExpansionPortDevice.NONE,
            ExpansionPortDevice.ZAPPER,
            ExpansionPortDevice.FOUR_PLAYER_ADAPTER,
        )

        fun show(emulator: Emulator, onSave: () -> Unit) {
            val dialog = ControllerConfig(emulator, onSave)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}