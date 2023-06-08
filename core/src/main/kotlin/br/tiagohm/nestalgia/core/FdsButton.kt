package br.tiagohm.nestalgia.core

enum class FdsButton(override val bit: Int) : ControllerButton {
    EJECT_DISK(2),
    INSERT_DISK(3),
}
