package br.tiagohm.nestalgia.core

enum class EmulationFlag(@JvmField val code: Long) : Flag {
    PAUSED(0x01),
    ALLOW_INVALID_INPUT(0x08),
    REMOVE_SPRITE_LIMIT(0x10),

    ENABLE_PPU_OAM_ROW_CORRUPTION(0x0200),

    ALLOW_BACKGROUND_INPUT(0x0400),

    FDS_AUTO_LOAD_DISK(0x4000),
    MMC3_IRQ_ALT_BEHAVIOR(0x8000),

    SWAP_DUTY_CYCLES(0x10000),

    AUTO_CONFIGURE_INPUT(0x40000),

    SILENCE_TRIANGLE_HIGH_FREQ(0x100000),
    REDUCE_DMC_POPPING(0x200000),

    DISABLE_BACKGROUND(0x400000),
    DISABLE_SPRITES(0x800000),
    FORCE_BACKGROUND_FIRST_COLUMN(0x1000000),
    FORCE_SPRITES_FIRST_COLUMN(0x2000000),
    DISABLE_PPU_2004_READS(0x4000000),
    DISABLE_NOISE_MODE_FLAG(0x8000000),
    DISABLE_PALETTE_READ(0x10000000),
    DISABLE_OAM_ADDR_BUG(0x20000000),
    DISABLE_PPU_RESET(0x40000000),
    ENABLE_OAM_DECAY(0x80000000),

    FDS_AUTO_INSERT_DISK(0x800000000),

    REWIND(0x1000000000),
    TURBO(0x2000000000),
    IN_BACKGROUND(0x4000000000),
    NSF_PLAYER_ENABLED(0x8000000000),

    USE_CUSTOM_VS_PALETTE(0x40000000000),

    ADAPTIVE_SPRITE_LIMIT(0x80000000000),

    ENABLE_PPU_2006_SCROLL_GLITCH(0x100000000000),
    ENABLE_PPU_2000_SCROLL_GLITCH(0x200000000000),

    NSF_REPEAT(0x800000000000),
    NSF_SHUFFLE(0x1000000000000),

    INTEGER_FPS_MODE(0x2000000000000),

    RANDOMIZE_MAPPER_POWER_ON_STATE(0x20000000000000),

    RANDOMIZE_CPU_PPU_ALIGNMENT(0x800000000000000),

    FORCE_MAX_SPEED(0x4000000000000000),
}
