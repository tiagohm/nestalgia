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
    final override val presets = mapOf("WASD" to StandardController.wasd(), "ARROW" to StandardController.arrow())
    override val defaultKeyMapping = presets["ARROW"]!!

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
