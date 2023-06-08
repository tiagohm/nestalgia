package br.tiagohm.nestalgia.core

enum class PpuModel {
    PPU_2C02,
    PPU_2C03,
    PPU_2C04A,
    PPU_2C04B,
    PPU_2C04C,
    PPU_2C04D,
    PPU_2C05A,
    PPU_2C05B,
    PPU_2C05C,
    PPU_2C05D,
    PPU_2C05E;

    val is2C02
        get() = this == PPU_2C02

    val is2C03
        get() = this == PPU_2C03

    val is2C04
        get() = this in PPU_2C04A..PPU_2C04D

    val is2C05
        get() = this in PPU_2C05A..PPU_2C05E
}
