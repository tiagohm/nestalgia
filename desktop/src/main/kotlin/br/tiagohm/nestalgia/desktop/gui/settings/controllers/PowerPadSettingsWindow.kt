package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.ControllerType.POWER_PAD_SIDE_A
import br.tiagohm.nestalgia.core.ControllerType.POWER_PAD_SIDE_B

class PowerPadSettingsWindow(
    override val keyMapping: KeyMapping,
    private val type: ControllerType,
) : AbstractControllerWindow() {

    override val buttons = PowerPad.Button.entries

    override fun defaultKey(button: ControllerButton) = when (button as PowerPad.Button) {
        PowerPad.Button.B01 -> KeyboardKeys.R
        PowerPad.Button.B02 -> KeyboardKeys.T
        PowerPad.Button.B03 -> KeyboardKeys.Y
        PowerPad.Button.B04 -> KeyboardKeys.U
        PowerPad.Button.B05 -> KeyboardKeys.F
        PowerPad.Button.B06 -> KeyboardKeys.G
        PowerPad.Button.B07 -> KeyboardKeys.H
        PowerPad.Button.B08 -> KeyboardKeys.J
        PowerPad.Button.B09 -> KeyboardKeys.V
        PowerPad.Button.B10 -> KeyboardKeys.B
        PowerPad.Button.B11 -> KeyboardKeys.N
        PowerPad.Button.B12 -> KeyboardKeys.M
    }

    override fun onCreate() {
        title = if (type == POWER_PAD_SIDE_A || type == POWER_PAD_SIDE_B) "Power Pad" else "Family Trainer Mat"

        super.onCreate()
    }
}
