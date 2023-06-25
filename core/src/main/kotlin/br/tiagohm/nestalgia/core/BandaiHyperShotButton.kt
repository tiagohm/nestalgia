package br.tiagohm.nestalgia.core

enum class BandaiHyperShotButton(override val bit: Int) : ControllerButton, HasCustomKey {
    FIRE(9);

    override val keyIndex = 2
}
