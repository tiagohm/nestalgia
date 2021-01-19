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

    inline val is2C02: Boolean
        get() = this == PPU_2C02

    inline val is2C03: Boolean
        get() = this == PPU_2C03

    inline val is2C04: Boolean
        get() = ordinal >= PPU_2C04A.ordinal && ordinal <= PPU_2C04D.ordinal

    inline val is2C05: Boolean
        get() = ordinal >= PPU_2C05A.ordinal && ordinal <= PPU_2C05E.ordinal
}