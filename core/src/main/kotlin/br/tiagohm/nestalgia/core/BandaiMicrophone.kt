package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.BandaiMicrophoneButton.*
import br.tiagohm.nestalgia.core.ControllerType.*

class BandaiMicrophone(console: Console) : ControlDevice(console, BANDAI_MICROPHONE, MAPPER_INPUT_PORT) {

    override fun setStateFromInput() {
        val keyMapping = console.settings.mapperPort.keyMapping

        setPressedState(A, keyMapping.customKey(A))
        setPressedState(B, keyMapping.customKey(B))

        if (console.frameCount % 2 == 0) {
            // 1-bit ADC microphone input.
            // Alternate between 1 and 0s (not sure if the game does anything with this data?).
            setPressedState(MICROPHONE, keyMapping.customKey(MICROPHONE))
        }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr in 0x6000..0x7FFF) {
            (if (isPressed(A)) 0 else 0x01) or
                (if (isPressed(B)) 0 else 0x02) or
                if (isPressed(MICROPHONE)) 0x04 else 0
        } else {
            0
        }
    }
}
