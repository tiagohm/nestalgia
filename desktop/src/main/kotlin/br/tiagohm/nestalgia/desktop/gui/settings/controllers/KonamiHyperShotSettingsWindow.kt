package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KonamiHyperShot

class KonamiHyperShotSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = KonamiHyperShot.Button.entries
    override val defaultKeyMapping = KonamiHyperShot.defaultKeyMapping()

    override fun onCreate() {
        title = "Konami Hyper Shot"

        super.onCreate()
    }
}
