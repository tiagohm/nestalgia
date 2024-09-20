package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.Zapper.Companion.AIM_OFFSCREEN_CUSTOM_KEY

class ZapperSettingsWindow(
    private val zapperDetectionRadius: IntArray,
    override val keyMapping: KeyMapping,
    private val port: Int,
) : AbstractControllerWindow() {

    override val buttons = Zapper.Button.entries
    override val defaultKeyMapping = Zapper.defaultKeyMapping()

    override fun buttonKeys(button: ControllerButton) = MouseButton.entries + Key.UNDEFINED

    override fun onCreate() {
        title = "Zapper"

        super.onCreate()
    }

    override fun onStart() {
        super.onStart()

        addKey("Aim offscreen", keyMapping.customKey(AIM_OFFSCREEN_CUSTOM_KEY), AIM_OFFSCREEN_CUSTOM_KEY, MouseButton.entries + Key.UNDEFINED)

        addSlider("Light detection radius (px)", 1.0, 16.0, zapperDetectionRadius[port].toDouble()) {
            zapperDetectionRadius[port] = it.toInt()
        }
    }
}
