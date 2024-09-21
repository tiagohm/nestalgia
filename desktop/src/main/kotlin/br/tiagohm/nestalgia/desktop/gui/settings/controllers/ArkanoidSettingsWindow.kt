package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.*

class ArkanoidSettingsWindow(
    private val arkanoidSensibility: IntArray,
    override val keyMapping: KeyMapping,
    private val port: Int,
) : AbstractControllerWindow() {

    override val buttons = ArkanoidController.Button.entries
    override val defaultKeyMapping = ArkanoidController.defaultKeyMapping()

    override fun buttonKeys(button: ControllerButton) = MouseButton.entries + Key.UNDEFINED

    override fun onCreate() {
        title = "Arkanoid"

        super.onCreate()
    }

    override fun onStart() {
        super.onStart()

        addSlider("Sensibility", 0.0, 3.0, arkanoidSensibility[port].toDouble()) {
            arkanoidSensibility[port] = it.toInt()
        }
    }
}
