package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerButton
import br.tiagohm.nestalgia.core.ControllerType
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.StandardController
import java.util.concurrent.atomic.AtomicInteger

open class StandardControllerSettingsWindow(
    override val keyMapping: KeyMapping,
    private val turboSpeed: AtomicInteger? = null,
    private val type: ControllerType = NES_CONTROLLER,
) : AbstractControllerWindow() {

    override val buttons: Iterable<ControllerButton> = StandardController.Button.entries
    override val presets = mapOf("WASD" to KeyMapping.wasd(), "ARROW" to KeyMapping.arrowKeys())

    override fun defaultKey(button: ControllerButton) = presets["ARROW"]?.key(button)

    override fun onCreate() {
        title = when (type) {
            HORI_TRACK -> "Hori Track"
            BANDAI_HYPER_SHOT -> "Bandai Hyper Shot"
            PACHINKO -> "Pachinko"
            else -> "NES/Famicom Controller"
        }

        super.onCreate()
    }

    override fun onStart() {
        super.onStart()

        if (turboSpeed != null) {
            addSlider("Turbo speed", 0.0, 4.0, turboSpeed.get().toDouble()) {
                turboSpeed.set(it.toInt())
            }
        }
    }
}
