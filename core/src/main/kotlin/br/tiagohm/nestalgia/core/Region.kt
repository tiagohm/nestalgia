package br.tiagohm.nestalgia.core

enum class Region {
    AUTO,
    NTSC,
    PAL,
    DENDY;

    inline val clockRate: Int
        get() = when (this) {
            PAL -> Cpu.CLOCK_RATE_PAL
            DENDY -> Cpu.CLOCK_RATE_DENDY
            else -> Cpu.CLOCK_RATE_NTSC
        }
}
