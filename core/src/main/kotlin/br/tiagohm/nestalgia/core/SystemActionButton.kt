package br.tiagohm.nestalgia.core

enum class SystemActionButton(override val bit: Int) : ControllerButton, HasCustomKey {
    RESET(0),
    POWER(1);

    override val keyIndex = 93 + ordinal
}
