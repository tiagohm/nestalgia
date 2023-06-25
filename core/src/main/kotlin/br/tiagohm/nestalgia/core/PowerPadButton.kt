package br.tiagohm.nestalgia.core

enum class PowerPadButton(override val bit: Int) : ControllerButton, HasCustomKey {
    B01(0),
    B02(1),
    B03(2),
    B04(3),
    B05(4),
    B06(5),
    B07(6),
    B08(7),
    B09(8),
    B10(9),
    B11(10),
    B12(11);

    override val keyIndex = 18 + ordinal
}
