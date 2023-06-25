package br.tiagohm.nestalgia.core

enum class ArkanoidButton(override val bit: Int) : ControllerButton, HasCustomKey {
    FIRE(0);

    override val keyIndex = 1
}
