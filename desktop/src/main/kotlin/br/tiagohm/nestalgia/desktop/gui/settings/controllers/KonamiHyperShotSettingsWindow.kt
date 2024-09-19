package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerButton
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.KeyboardKeys
import br.tiagohm.nestalgia.core.KonamiHyperShot

class KonamiHyperShotSettingsWindow(override val keyMapping: KeyMapping) : AbstractControllerWindow() {

    override val buttons = KonamiHyperShot.Button.entries

    override fun defaultKey(button: ControllerButton) = when (button as KonamiHyperShot.Button) {
        KonamiHyperShot.Button.RUN_P1 -> KeyboardKeys.A
        KonamiHyperShot.Button.JUMP_P1 -> KeyboardKeys.S
        KonamiHyperShot.Button.RUN_P2 -> KeyboardKeys.K
        KonamiHyperShot.Button.JUMP_P2 -> KeyboardKeys.L
    }

    override fun onCreate() {
        title = "Konami Hyper Shot"

        super.onCreate()
    }
}
