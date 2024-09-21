package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.*

class SuborMouseSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = SuborMouse.Button.entries
    override val defaultKeyMapping = SuborMouse.defaultKeyMapping()

    override fun buttonKeys(button: ControllerButton) = MouseButton.entries + Key.UNDEFINED

    override fun onCreate() {
        title = "Subor Mouse"

        super.onCreate()
    }
}
