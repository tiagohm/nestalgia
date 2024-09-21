package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ExcitingBoxingController
import br.tiagohm.nestalgia.core.KeyMapping

class ExcitingBoxingSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = ExcitingBoxingController.Button.entries
    override val defaultKeyMapping = ExcitingBoxingController.defaultKeyMapping()

    override fun onCreate() {
        title = "Exciting Boxing Punching Bag"

        super.onCreate()
    }
}
