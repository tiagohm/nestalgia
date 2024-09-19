package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerButton
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.core.PartyTap

class PartyTapSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = PartyTap.Button.entries

    override fun defaultKey(button: ControllerButton) = when (button as PartyTap.Button) {
        PartyTap.Button.B1 -> KeyboardKeys.NUMBER_1
        PartyTap.Button.B2 -> KeyboardKeys.NUMBER_2
        PartyTap.Button.B3 -> KeyboardKeys.NUMBER_3
        PartyTap.Button.B4 -> KeyboardKeys.NUMBER_4
        PartyTap.Button.B5 -> KeyboardKeys.NUMBER_5
        PartyTap.Button.B6 -> KeyboardKeys.NUMBER_6
    }

    override fun onCreate() {
        title = "Party Tap"

        super.onCreate()
    }
}
