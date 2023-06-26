package br.tiagohm.nestalgia.desktop.gui.converters

import br.tiagohm.nestalgia.core.ArkanoidController
import br.tiagohm.nestalgia.core.ControllerButton
import br.tiagohm.nestalgia.core.StandardController
import br.tiagohm.nestalgia.core.Zapper
import javafx.util.StringConverter

object ControllerButtonStringConverter : StringConverter<ControllerButton>() {

    override fun toString(key: ControllerButton?) = if (key == null) "-" else LABELS[key] ?: "$key"

    override fun fromString(text: String?) = null

    @JvmStatic private val LABELS = mapOf(
        StandardController.Button.A to "A",
        StandardController.Button.B to "B",
        StandardController.Button.UP to "Up",
        StandardController.Button.LEFT to "Left",
        StandardController.Button.RIGHT to "Right",
        StandardController.Button.DOWN to "Down",
        StandardController.Button.START to "Start",
        StandardController.Button.SELECT to "Select",
        StandardController.Button.TURBO_A to "Turbo (A)",
        StandardController.Button.TURBO_B to "Turbo (B)",
        Zapper.Button.FIRE to "Fire",
        ArkanoidController.Button.FIRE to "Fire",
    )
}
