package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ArkanoidController.Button.FIRE

// https://www.nesdev.org/wiki/Arkanoid_controller

class ArkanoidController(
    console: Console, type: ControllerType, port: Int,
    private val keyMapping: KeyMapping,
) : ControlDevice(console, type, port) {


    enum class Button : ControllerButton, HasCustomKey {
        FIRE;

        override val bit = ordinal

        override val keyIndex = 1
    }

    @Volatile private var currentValue = (0xF4 - 0x54) / 2
    @Volatile private var stateBuffer = 0
    private val sensibility = SENSIBILITY_PX[console.settings.arkanoidSensibility[port]]

    override fun setStateFromInput() {
        pressedStateFromKeys()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun pressedStateFromKeys() {
        setPressedState(FIRE, keyMapping.key(FIRE))
    }

    override fun refreshStateBuffer() {
        // Map range [0, 255] to [0x54, 0xF4].
        // var newValue = (5 * console.keyManager.mouseX + 672) / 8
        var newValue = (80 * console.keyManager.mouseX - 164 * sensibility + 10752) / (128 - sensibility)

        if (newValue < 0x54) {
            newValue = 0x54
        } else if (newValue > 0xF4) {
            newValue = 0xF4
        }

        currentValue = newValue
        stateBuffer = newValue
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        var output = 0

        // Famicom.
        if (isExpansionDevice) {
            if (addr == 0x4016) {
                // Fire button is on port 1.
                if (isPressed(FIRE)) {
                    output = 0x02
                }
            } else if (addr == 0x4017) {
                // Serial data is on port 2.
                output = stateBuffer.inv() shr 6 and 0x02
                stateBuffer = stateBuffer shl 1
            }
        }
        // NES.
        else if (isCurrentPort(addr)) {
            output = stateBuffer.inv() shr 3 and 0x10

            stateBuffer = stateBuffer shl 1

            if (isPressed(FIRE)) {
                output = output or 0x08
            }
        }

        return output
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobeOnWrite(value)
    }

    companion object {

        private val SENSIBILITY_PX = intArrayOf(0, 16, 32, 64)
    }
}
