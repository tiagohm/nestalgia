package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerButton
import br.tiagohm.nestalgia.core.ControllerType.PACHINKO
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.Pachinko

open class PachinkoSettingsWindow(keyMapping: KeyMapping) : StandardControllerSettingsWindow(keyMapping, null, PACHINKO) {

    override val buttons: Iterable<ControllerButton> = super.buttons.filter { (it as Enum<*>).ordinal < 8 } + Pachinko.Button.entries
    override val defaultKeyMapping = Pachinko.defaultKeyMapping()
}
