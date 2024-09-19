package br.tiagohm.nestalgia.core

enum class SystemActionButton : ControllerButton, HasCustomKey {
    RESET,
    POWER;

    override val bit = ordinal

    override val keyIndex = 93 + ordinal
}
