package br.tiagohm.nestalgia.core

enum class ArkanoidButton(override val bit: Int) : ControllerButton {
    FIRE(0);

    override val isCustomKey
        get() = true
}