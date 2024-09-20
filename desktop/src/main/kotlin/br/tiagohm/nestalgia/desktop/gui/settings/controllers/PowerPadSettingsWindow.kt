package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerType
import br.tiagohm.nestalgia.core.ControllerType.POWER_PAD_SIDE_A
import br.tiagohm.nestalgia.core.ControllerType.POWER_PAD_SIDE_B
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.PowerPad

class PowerPadSettingsWindow(
    override val keyMapping: KeyMapping,
    private val type: ControllerType,
) : AbstractControllerWindow() {

    override val buttons = PowerPad.Button.entries
    override val defaultKeyMapping = PowerPad.defaultKeyMapping()

    override fun onCreate() {
        title = if (type == POWER_PAD_SIDE_A || type == POWER_PAD_SIDE_B) "Power Pad" else "Family Trainer Mat"

        super.onCreate()
    }
}
