package br.tiagohm.nestalgia.core

enum class StandardControllerButton(override val bit: Int) : Button {
    UP(0),
    DOWN(1),
    LEFT(2),
    RIGHT(3),
    START(4),
    SELECT(5),
    B(6),
    A(7),
    MICROPHONE(8),
}