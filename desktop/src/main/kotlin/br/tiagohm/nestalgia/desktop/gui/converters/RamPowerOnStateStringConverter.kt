package br.tiagohm.nestalgia.desktop.gui.converters

import br.tiagohm.nestalgia.core.RamPowerOnState
import br.tiagohm.nestalgia.core.RamPowerOnState.*
import javafx.util.StringConverter

object RamPowerOnStateStringConverter : StringConverter<RamPowerOnState>() {

    override fun toString(key: RamPowerOnState?) = when (key) {
        ALL_ZEROS -> "All 0s"
        ALL_ONES -> "All 1s"
        RANDOM -> "Random"
        null -> "-"
    }

    override fun fromString(text: String?) = null
}
