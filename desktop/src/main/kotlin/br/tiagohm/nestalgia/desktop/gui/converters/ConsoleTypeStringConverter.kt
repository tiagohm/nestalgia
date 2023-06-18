package br.tiagohm.nestalgia.desktop.gui.converters

import br.tiagohm.nestalgia.core.ConsoleType
import br.tiagohm.nestalgia.core.ConsoleType.*
import javafx.util.StringConverter

object ConsoleTypeStringConverter : StringConverter<ConsoleType>() {

    override fun toString(key: ConsoleType?) = when (key) {
        NES_001 -> "NES - Front loader (NES-001)"
        NES_101 -> "NES - Top loader (NES-101)"
        HVC_001 -> "Famicom (HVC-001)"
        HVC_101 -> "AV Famicom (HVC-101)"
        null -> "-"
    }

    override fun fromString(text: String?) = null
}
