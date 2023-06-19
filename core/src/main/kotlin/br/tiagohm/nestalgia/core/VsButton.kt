package br.tiagohm.nestalgia.core

enum class VsButton(override val bit: Int) : ControllerButton {
    INSERT_COIN_1(2),
    INSERT_COIN_2(3),
    SERVICE(4);

    override val isCustomKey
        get() = true
}
