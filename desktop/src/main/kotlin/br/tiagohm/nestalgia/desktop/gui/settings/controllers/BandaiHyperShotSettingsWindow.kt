package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.BandaiHyperShot.Companion.AIM_OFFSCREEN_CUSTOM_KEY
import br.tiagohm.nestalgia.core.ControllerType.BANDAI_HYPER_SHOT
import java.util.concurrent.atomic.AtomicInteger

class BandaiHyperShotSettingsWindow(keyMapping: KeyMapping, turboSpeed: AtomicInteger) : StandardControllerSettingsWindow(keyMapping, turboSpeed, BANDAI_HYPER_SHOT) {

    override val buttons: Iterable<ControllerButton> = super.buttons.filter { (it as Enum<*>).ordinal < 8 } + BandaiHyperShot.Button.entries
    override val defaultKeyMapping = BandaiHyperShot.defaultKeyMapping()

    override fun buttonKeys(button: ControllerButton) = if (button is BandaiHyperShot.Button) MouseButton.entries + Key.UNDEFINED else super.buttonKeys(button)

    override fun onStart() {
        super.onStart()

        addKey("Aim offscreen", keyMapping.customKey(AIM_OFFSCREEN_CUSTOM_KEY), AIM_OFFSCREEN_CUSTOM_KEY, MouseButton.entries + Key.UNDEFINED)
    }
}
