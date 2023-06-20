package br.tiagohm.nestalgia.core

enum class ExcitingBoxingButton(override val bit: Int) : ControllerButton {
    HIT_BODY(5),
    HOOK_LEFT(0),
    HOOK_RIGHT(3),
    JAB_LEFT(4),
    JAB_RIGHT(6),
    MOVE_LEFT(2),
    MOVE_RIGHT(1),
    STRAIGHT(7);

    override val isCustomKey
        get() = true
}
