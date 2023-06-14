package br.tiagohm.nestalgia.core

enum class ZapperButton(override val bit: Int) : ControllerButton {
    FIRE(0),
    AIM_OFFSCREEN(1),
}
