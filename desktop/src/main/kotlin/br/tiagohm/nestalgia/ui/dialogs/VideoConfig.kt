package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.core.EmulationFlag
import br.tiagohm.nestalgia.core.Emulator
import br.tiagohm.nestalgia.core.PaletteType
import br.tiagohm.nestalgia.ui.*
import javax.swing.JPanel

@ExperimentalUnsignedTypes
class VideoConfig(
    private val emulator: Emulator,
    private val onSave: () -> Unit,
) : Dialog("Audio Settings") {

    private val settings = emulator.settings
    private var paletteType = settings.paletteType
    private var integerFpsMode = settings.checkFlag(EmulationFlag.INTEGER_FPS_MODE)
    private var removeSpriteLimit = settings.checkFlag(EmulationFlag.REMOVE_SPRITE_LIMIT)
    private var adaptiveSpriteLimit = settings.checkFlag(EmulationFlag.ADAPTIVE_SPRITE_LIMIT)
    private var disableBackground = settings.checkFlag(EmulationFlag.DISABLE_BACKGROUND)
    private var disableSprites = settings.checkFlag(EmulationFlag.DISABLE_SPRITES)
    private var forceBackgroundFirstColumn = settings.checkFlag(EmulationFlag.FORCE_BACKGROUND_FIRST_COLUMN)
    private var forceSpritesFirstColumn = settings.checkFlag(EmulationFlag.FORCE_SPRITES_FIRST_COLUMN)

    override val body: JPanel
        get() {
            return panel(9, 3, margin = margin(16)) {
                var row = 0

                checkbox(
                    row, 0,
                    integerFpsMode,
                    "Enable integer FPS mode",
                    onChanged = {
                        integerFpsMode = it
                    })

                row++

                label("Palette:", row, 0)
                dropdown(
                    row, 1,
                    PALETTE_TYPES,
                    paletteType,
                    colSpan = 2,
                    onChanged = {
                        paletteType = it
                    }
                )

                checkbox(
                    ++row,
                    0,
                    removeSpriteLimit,
                    "Remove sprite limit (Reduces flashing)",
                    onChanged = {
                        removeSpriteLimit = it
                        updateView()
                    })

                checkbox(
                    ++row, 0,
                    adaptiveSpriteLimit,
                    "Automatically re-enable sprite limit as needed to prevent graphical glitches when",
                    isEnabled = removeSpriteLimit,
                    onChanged = {
                        adaptiveSpriteLimit = it
                    })

                checkbox(
                    ++row, 0,
                    forceSpritesFirstColumn,
                    "Force sprite display in first column",
                    onChanged = {
                        forceSpritesFirstColumn = it
                    })

                checkbox(
                    ++row, 0,
                    forceBackgroundFirstColumn,
                    "Force background display in first column",
                    onChanged = {
                        forceBackgroundFirstColumn = it
                    })

                checkbox(
                    ++row, 0,
                    disableSprites,
                    "Disable sprites",
                    onChanged = {
                        disableSprites = it
                    })

                checkbox(
                    ++row, 0,
                    disableBackground,
                    "Disable background",
                    onChanged = {
                        disableBackground = it
                    })

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
        settings.paletteType = paletteType

        if (integerFpsMode) settings.setFlag(EmulationFlag.INTEGER_FPS_MODE)
        else settings.clearFlag(EmulationFlag.INTEGER_FPS_MODE)

        if (removeSpriteLimit) settings.setFlag(EmulationFlag.REMOVE_SPRITE_LIMIT)
        else settings.clearFlag(EmulationFlag.REMOVE_SPRITE_LIMIT)

        if (adaptiveSpriteLimit) settings.setFlag(EmulationFlag.ADAPTIVE_SPRITE_LIMIT)
        else settings.clearFlag(EmulationFlag.ADAPTIVE_SPRITE_LIMIT)

        if (disableBackground) settings.setFlag(EmulationFlag.DISABLE_BACKGROUND)
        else settings.clearFlag(EmulationFlag.DISABLE_BACKGROUND)

        if (disableSprites) settings.setFlag(EmulationFlag.DISABLE_SPRITES)
        else settings.clearFlag(EmulationFlag.DISABLE_SPRITES)

        if (forceBackgroundFirstColumn) settings.setFlag(EmulationFlag.FORCE_BACKGROUND_FIRST_COLUMN)
        else settings.clearFlag(EmulationFlag.FORCE_BACKGROUND_FIRST_COLUMN)

        if (forceSpritesFirstColumn) settings.setFlag(EmulationFlag.FORCE_SPRITES_FIRST_COLUMN)
        else settings.clearFlag(EmulationFlag.FORCE_SPRITES_FIRST_COLUMN)

        onSave()

        dispose()
    }

    companion object {
        private val PALETTE_TYPES = PaletteType.values().toList()

        fun show(emulator: Emulator, onSave: () -> Unit) {
            val dialog = VideoConfig(emulator, onSave)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}