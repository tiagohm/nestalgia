package br.tiagohm.nestalgia.core

enum class BandaiHyperShotButton(override val bit: Int) : ControllerButton {
    FIRE(9);

    override val isCustomKey
        get() = true
}
