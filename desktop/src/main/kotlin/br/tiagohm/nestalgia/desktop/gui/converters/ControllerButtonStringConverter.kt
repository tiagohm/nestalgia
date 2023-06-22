package br.tiagohm.nestalgia.desktop.gui.converters

import br.tiagohm.nestalgia.core.ArkanoidButton
import br.tiagohm.nestalgia.core.ControllerButton
import br.tiagohm.nestalgia.core.StandardControllerButton
import br.tiagohm.nestalgia.core.ZapperButton
import javafx.util.StringConverter

object ControllerButtonStringConverter : StringConverter<ControllerButton>() {

    override fun toString(key: ControllerButton?) = if (key == null) "-" else LABELS[key] ?: "$key"

    override fun fromString(text: String?) = null

    @JvmStatic private val LABELS = mapOf(
        StandardControllerButton.A to "A",
        StandardControllerButton.B to "B",
        StandardControllerButton.UP to "Up",
        StandardControllerButton.LEFT to "Left",
        StandardControllerButton.RIGHT to "Right",
        StandardControllerButton.DOWN to "Down",
        StandardControllerButton.START to "Start",
        StandardControllerButton.SELECT to "Select",
        StandardControllerButton.TURBO_A to "Turbo (A)",
        StandardControllerButton.TURBO_B to "Turbo (B)",
        ZapperButton.FIRE to "Fire",
        ArkanoidButton.FIRE to "Fire",
    )
}
