package br.tiagohm.nestalgia.core

enum class KonamiHyperShotButton(override val bit: Int) : ControllerButton {
    RUN_P1(0),
    JUMP_P1(1),
    RUN_P2(2),
    JUMP_P2(3);

    override val isCustomKey
        get() = true
}
