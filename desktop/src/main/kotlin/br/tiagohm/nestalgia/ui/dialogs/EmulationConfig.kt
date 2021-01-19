package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.core.EmulationFlag
import br.tiagohm.nestalgia.core.Emulator
import br.tiagohm.nestalgia.core.RamPowerOnState
import br.tiagohm.nestalgia.ui.*
import javax.swing.JPanel

@ExperimentalUnsignedTypes
class EmulationConfig(
    private val emulator: Emulator,
    private val onSave: () -> Unit,
) : Dialog("Emulation Settings") {

    private val settings = emulator.settings
    private var enablePpuOamRowCorruption = settings.checkFlag(EmulationFlag.ENABLE_PPU_OAM_ROW_CORRUPTION)
    private var enablePpu2000ScrollGlitch = settings.checkFlag(EmulationFlag.ENABLE_PPU_2000_SCROLL_GLITCH)
    private var enablePpu2006ScrollGlitch = settings.checkFlag(EmulationFlag.ENABLE_PPU_2006_SCROLL_GLITCH)
    private var randomizeCpuPpuAlignment = settings.checkFlag(EmulationFlag.RANDOMIZE_CPU_PPU_ALIGNMENT)
    private var randomizeMapperPowerOnState = settings.checkFlag(EmulationFlag.RANDOMIZE_MAPPER_POWER_ON_STATE)
    private var enableOamDecay = settings.checkFlag(EmulationFlag.ENABLE_OAM_DECAY)
    private var disablePaletteRead = settings.checkFlag(EmulationFlag.DISABLE_PALETTE_READ)
    private var disableOamAddrBug = settings.checkFlag(EmulationFlag.DISABLE_OAM_ADDR_BUG)
    private var disablePpuReset = settings.checkFlag(EmulationFlag.DISABLE_PPU_RESET)
    private var disablePpu2004Reads = settings.checkFlag(EmulationFlag.DISABLE_PPU_2004_READS)
    private var useNes101Hvc101Behavior = settings.checkFlag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)
    private var useAlternativeMmc3Irq = settings.checkFlag(EmulationFlag.MMC3_IRQ_ALT_BEHAVIOR)
    private var allowInvalidInput = settings.checkFlag(EmulationFlag.ALLOW_INVALID_INPUT)
    private var ramPowerOnState = settings.ramPowerOnState
    private var extraScanlinesBeforeNmi = settings.extraScanlinesBeforeNmi
    private var extraScanlinesAfterNmi = settings.extraScanlinesAfterNmi

    override val body: JPanel
        get() {
            return panel(18, 3, margin = margin(16)) {
                var row = 0

                checkbox(
                    row, 0,
                    enablePpuOamRowCorruption,
                    "Enable PPU OAM row corruption emulation",
                    onChanged = {
                        enablePpuOamRowCorruption = it
                    })

                checkbox(
                    ++row,
                    0,
                    enablePpu2000ScrollGlitch,
                    "Enable PPU \$2000/\$2005/\$2006 first-write scroll glitch emulation",
                    onChanged = {
                        enablePpu2000ScrollGlitch = it
                    })

                checkbox(
                    ++row, 0,
                    enablePpu2006ScrollGlitch,
                    "Enable PPU \$2006 write scroll glitch emulation",
                    onChanged = {
                        enablePpu2006ScrollGlitch = it
                    })

                checkbox(
                    ++row, 0,
                    randomizeCpuPpuAlignment,
                    "Randomize power-on/reset CPU/PPU alignment",
                    onChanged = {
                        randomizeCpuPpuAlignment = it
                    })

                checkbox(
                    ++row, 0,
                    randomizeMapperPowerOnState,
                    "Randomize power-on state for mappers",
                    onChanged = {
                        randomizeMapperPowerOnState = it
                    })

                row++

                label("Default power on state for RAM:", row, 0)
                dropdown(
                    row, 1,
                    RamPowerOnState.values().toList(),
                    ramPowerOnState,
                    colSpan = 2,
                    onChanged = {
                        ramPowerOnState = it
                    }
                )

                checkbox(
                    ++row, 0,
                    enableOamDecay,
                    "Enable OAM RAM decay",
                    onChanged = {
                        enableOamDecay = it
                    })

                checkbox(
                    ++row, 0,
                    disablePaletteRead,
                    "Disable PPU palette reads",
                    onChanged = {
                        disablePaletteRead = it
                    })

                checkbox(
                    ++row, 0,
                    disableOamAddrBug,
                    "Disable PPU OAMADDR bug emulation",
                    onChanged = {
                        disableOamAddrBug = it
                    })

                checkbox(
                    ++row, 0,
                    disablePpuReset,
                    "Do not reset PPU when resetting console (Famicom behavior)",
                    onChanged = {
                        disablePpuReset = it
                    })

                checkbox(
                    ++row, 0,
                    disablePpu2004Reads,
                    "Disable PPU \$2004 reads (Famicom behavior)",
                    onChanged = {
                        disablePpu2004Reads = it
                    })

                checkbox(
                    ++row, 0,
                    useNes101Hvc101Behavior,
                    "Use NES/HVC-101 (Top-loader / AV Famicom) behavior",
                    onChanged = {
                        useNes101Hvc101Behavior = it
                    })

                checkbox(
                    ++row, 0,
                    useAlternativeMmc3Irq,
                    "Use alternative MMC3 IRQ behavior",
                    onChanged = {
                        useAlternativeMmc3Irq = it
                    })

                checkbox(
                    ++row, 0,
                    allowInvalidInput,
                    "Allow invalid input (e.g Down + Up or Left + Right at the same time)",
                    onChanged = {
                        allowInvalidInput = it
                    })

                label("Additional scanlines before NMI:", ++row, 0)
                spinnerNumber(
                    row, 1,
                    extraScanlinesBeforeNmi, 0, 1000,
                    colSpan = 2,
                    onChanged = {
                        extraScanlinesBeforeNmi = it
                    }
                )

                label("Additional scanlines after NMI:", ++row, 0)
                spinnerNumber(
                    row, 1,
                    extraScanlinesAfterNmi, 0, 1000,
                    colSpan = 2,
                    onChanged = {
                        extraScanlinesAfterNmi = it
                    }
                )

                button(
                    "Save",
                    ++row, 0,
                    colSpan = 3,
                    fill = Fill.NONE,
                    anchor = Anchor.CENTER,
                    minimumSize = Size(100),
                    onClick = ::onOk
                )
            }
        }

    private fun onOk() {
        if (enablePpuOamRowCorruption) settings.setFlag(EmulationFlag.ENABLE_PPU_OAM_ROW_CORRUPTION)
        else settings.clearFlag(EmulationFlag.ENABLE_PPU_OAM_ROW_CORRUPTION)

        if (enablePpu2000ScrollGlitch) settings.setFlag(EmulationFlag.ENABLE_PPU_2000_SCROLL_GLITCH)
        else settings.clearFlag(EmulationFlag.ENABLE_PPU_2000_SCROLL_GLITCH)

        if (enablePpu2006ScrollGlitch) settings.setFlag(EmulationFlag.ENABLE_PPU_2006_SCROLL_GLITCH)
        else settings.clearFlag(EmulationFlag.ENABLE_PPU_2006_SCROLL_GLITCH)

        if (randomizeCpuPpuAlignment) settings.setFlag(EmulationFlag.RANDOMIZE_CPU_PPU_ALIGNMENT)
        else settings.clearFlag(EmulationFlag.RANDOMIZE_CPU_PPU_ALIGNMENT)

        if (randomizeMapperPowerOnState) settings.setFlag(EmulationFlag.RANDOMIZE_MAPPER_POWER_ON_STATE)
        else settings.clearFlag(EmulationFlag.RANDOMIZE_MAPPER_POWER_ON_STATE)

        if (enableOamDecay) settings.setFlag(EmulationFlag.ENABLE_OAM_DECAY)
        else settings.clearFlag(EmulationFlag.ENABLE_OAM_DECAY)

        if (disablePaletteRead) settings.setFlag(EmulationFlag.DISABLE_PALETTE_READ)
        else settings.clearFlag(EmulationFlag.DISABLE_PALETTE_READ)

        if (disableOamAddrBug) settings.setFlag(EmulationFlag.DISABLE_OAM_ADDR_BUG)
        else settings.clearFlag(EmulationFlag.DISABLE_OAM_ADDR_BUG)

        if (disablePpuReset) settings.setFlag(EmulationFlag.DISABLE_PPU_RESET)
        else settings.clearFlag(EmulationFlag.DISABLE_PPU_RESET)

        if (disablePpu2004Reads) settings.setFlag(EmulationFlag.DISABLE_PPU_2004_READS)
        else settings.clearFlag(EmulationFlag.DISABLE_PPU_2004_READS)

        if (useNes101Hvc101Behavior) settings.setFlag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)
        else settings.clearFlag(EmulationFlag.USE_NES_101_HVC_101_BEHAVIOR)

        if (useAlternativeMmc3Irq) settings.setFlag(EmulationFlag.MMC3_IRQ_ALT_BEHAVIOR)
        else settings.clearFlag(EmulationFlag.MMC3_IRQ_ALT_BEHAVIOR)

        if (allowInvalidInput) settings.setFlag(EmulationFlag.ALLOW_INVALID_INPUT)
        else settings.clearFlag(EmulationFlag.ALLOW_INVALID_INPUT)

        settings.ramPowerOnState = ramPowerOnState
        settings.extraScanlinesAfterNmi = extraScanlinesAfterNmi
        settings.extraScanlinesBeforeNmi = extraScanlinesBeforeNmi

        onSave()

        dispose()
    }

    companion object {
        fun show(emulator: Emulator, onSave: () -> Unit) {
            val dialog = EmulationConfig(emulator, onSave)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}