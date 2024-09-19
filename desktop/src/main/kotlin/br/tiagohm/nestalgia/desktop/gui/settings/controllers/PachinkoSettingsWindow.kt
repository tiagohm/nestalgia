package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.ControllerType.PACHINKO

open class PachinkoSettingsWindow(keyMapping: KeyMapping) : StandardControllerSettingsWindow(keyMapping, null, PACHINKO) {

    override val buttons: Iterable<ControllerButton> = super.buttons.filter { (it as Enum<*>).ordinal < 8 } + Pachinko.Button.entries

    override fun defaultKey(button: ControllerButton) = when (button) {
        is StandardController.Button -> super.defaultKey(button)
        Pachinko.Button.PRESS -> KeyboardKeys.F
        Pachinko.Button.RELEASE -> KeyboardKeys.R
        else -> null
    }
}
