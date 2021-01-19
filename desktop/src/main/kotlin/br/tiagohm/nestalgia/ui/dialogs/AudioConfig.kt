package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.core.EmulationFlag
import br.tiagohm.nestalgia.core.Emulator
import br.tiagohm.nestalgia.ui.*
import javax.swing.JPanel

@ExperimentalUnsignedTypes
class AudioConfig(
    private val emulator: Emulator,
    private val onSave: () -> Unit,
) : Dialog("Audio Settings") {

    private val settings = emulator.settings
    private var sampleRate = settings.sampleRate
    private var disableNoiseModeFlag = settings.checkFlag(EmulationFlag.DISABLE_NOISE_MODEL_FLAG)
    private var silenceTriangleHighFreq = settings.checkFlag(EmulationFlag.SILENCE_TRIANGLE_HIGH_FREQ)
    private var swapDutyCycles = settings.checkFlag(EmulationFlag.SWAP_DUTY_CYCLES)
    private var reduceDmcPopping = settings.checkFlag(EmulationFlag.REDUCE_DMC_POPPING)

    override val body: JPanel
        get() {
            return panel(6, 3, margin = margin(16)) {
                var row = 0

                label("Sample Rate (Hz)", row, 0)
                dropdown(
                    row, 1,
                    listOf(11025, 22050, 44100, 48000, 96000),
                    sampleRate,
                    colSpan = 2,
                    onChanged = {
                        sampleRate = it
                    }
                )

                checkbox(
                    ++row, 0,
                    disableNoiseModeFlag,
                    "Disable noise channel mode flag",
                    onChanged = {
                        disableNoiseModeFlag = it
                    })

                checkbox(
                    ++row,
                    0,
                    silenceTriangleHighFreq,
                    "Mute ultrasonic frequencies on triangle channel (reduces popping)",
                    onChanged = {
                        silenceTriangleHighFreq = it
                    })

                checkbox(
                    ++row, 0,
                    swapDutyCycles,
                    "Swap square channels duty cycles (Mimics old clones)",
                    onChanged = {
                        swapDutyCycles = it
                    })

                checkbox(
                    ++row, 0,
                    reduceDmcPopping,
                    "Reduce popping sounds on the DMC channel",
                    onChanged = {
                        reduceDmcPopping = it
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
        settings.sampleRate = sampleRate

        if (disableNoiseModeFlag) settings.setFlag(EmulationFlag.DISABLE_NOISE_MODEL_FLAG)
        else settings.clearFlag(EmulationFlag.DISABLE_NOISE_MODEL_FLAG)

        if (silenceTriangleHighFreq) settings.setFlag(EmulationFlag.SILENCE_TRIANGLE_HIGH_FREQ)
        else settings.clearFlag(EmulationFlag.SILENCE_TRIANGLE_HIGH_FREQ)

        if (swapDutyCycles) settings.setFlag(EmulationFlag.SWAP_DUTY_CYCLES)
        else settings.clearFlag(EmulationFlag.SWAP_DUTY_CYCLES)

        if (reduceDmcPopping) settings.setFlag(EmulationFlag.REDUCE_DMC_POPPING)
        else settings.clearFlag(EmulationFlag.REDUCE_DMC_POPPING)

        onSave()

        dispose()
    }

    companion object {
        fun show(emulator: Emulator, onSave: () -> Unit) {
            val dialog = AudioConfig(emulator, onSave)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}