package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.SuborKeyboard

class SuborKeyboardSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = SuborKeyboard.Button.entries
    override val defaultKeyMapping = SuborKeyboard.defaultKeyMapping()

    override fun onCreate() {
        title = "Subor Keyboard"

        super.onCreate()
    }
}
