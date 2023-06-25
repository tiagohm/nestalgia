package br.tiagohm.nestalgia.core

enum class FdsButton(override val bit: Int) : ControllerButton, HasCustomKey {
    EJECT_DISK(2),
    INSERT_DISK(3);

    override val keyIndex = 95 + ordinal
}
