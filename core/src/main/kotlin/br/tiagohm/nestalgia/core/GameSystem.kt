package br.tiagohm.nestalgia.core

enum class GameSystem {
    NTSC,
    PAL,
    FAMICOM,
    DENDY,
    VS_SYSTEM,
    PLAY_CHOICE,
    FDS,
    UNKNOWN;

    val isFamicom
        get() = this == FAMICOM || this == FDS || this == DENDY
}
