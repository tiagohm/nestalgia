package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.PartyTap

class PartyTapSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = PartyTap.Button.entries
    override val defaultKeyMapping = PartyTap.defaultKeyMapping()

    override fun onCreate() {
        title = "Party Tap"

        super.onCreate()
    }
}
