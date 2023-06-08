package br.tiagohm.nestalgia.core

enum class SystemActionButton(override val bit: Int) : ControllerButton {
    RESET(0),
    POWER(1),
}
