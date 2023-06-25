package br.tiagohm.nestalgia.core

enum class VsButton(override val bit: Int) : ControllerButton, HasCustomKey {
    INSERT_COIN_1(2),
    INSERT_COIN_2(3),
    SERVICE(4);

    override val keyIndex = 97 + ordinal
}
