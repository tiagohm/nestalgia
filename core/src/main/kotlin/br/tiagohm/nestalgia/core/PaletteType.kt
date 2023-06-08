package br.tiagohm.nestalgia.core

enum class PaletteType(override val data: IntArray) : Palette {
    DEFAULT(EmulationSettings.DEFAULT_PALETTE),
    UNSATURATED(EmulationSettings.UNSATURATED_PALETTE),
    YUV(EmulationSettings.YUV_PALETTE),
    NESTOPIA(EmulationSettings.NESTOPIA_PALETTE),
    COMPOSITE_DIRECT(EmulationSettings.COMPOSITE_DIRECT_PALETTE),
    NES_CLASSIC(EmulationSettings.NES_CLASSIC_PALETTE),
    ORIGINAL_HARDWARE(EmulationSettings.ORIGINAL_HARDWARE_PALETTE),
    PVM_STYLE(EmulationSettings.PVM_STYLE_PALETTE),
    SONY_CXA_2025(EmulationSettings.SONY_CXA_2025_PALETTE),
    WAVEBEAM(EmulationSettings.WAVEBEAM_PALETTE);
}
