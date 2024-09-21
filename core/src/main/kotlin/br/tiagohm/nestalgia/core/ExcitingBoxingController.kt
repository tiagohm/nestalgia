package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.EXCITING_BOXING
import br.tiagohm.nestalgia.core.ExcitingBoxingController.Button.*
import br.tiagohm.nestalgia.core.KonamiHyperShot.Button

class ExcitingBoxingController(
    console: Console,
    private val keyMapping: KeyMapping,
) : ControlDevice(console, EXCITING_BOXING, EXP_DEVICE_PORT) {

    enum class Button(override val bit: Int) : ControllerButton, HasCustomKey {
        HIT_BODY(5),
        HOOK_LEFT(0),
        HOOK_RIGHT(3),
        JAB_LEFT(4),
        JAB_RIGHT(6),
        MOVE_LEFT(2),
        MOVE_RIGHT(1),
        STRAIGHT(7);

        override val keyIndex = 10 + ordinal
    }

    @Volatile private var selectedSensors = false
    private val keys = Button.entries.map(keyMapping::customKey).toTypedArray()

    override fun setStateFromInput() {
        Button.entries.forEach { setPressedState(it, keys[it.ordinal]) }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr == 0x4017) {
            if (selectedSensors) {
                (if (isPressed(JAB_LEFT)) 0 else 0x02) or
                    (if (isPressed(HIT_BODY)) 0 else 0x04) or
                    (if (isPressed(JAB_RIGHT)) 0 else 0x08) or
                    if (isPressed(STRAIGHT)) 0 else 0x10
            } else {
                (if (isPressed(HOOK_LEFT)) 0 else 0x02) or
                    (if (isPressed(MOVE_RIGHT)) 0 else 0x04) or
                    (if (isPressed(MOVE_LEFT)) 0 else 0x08) or
                    if (isPressed(HOOK_RIGHT)) 0 else 0x10
            }
        } else {
            0
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        selectedSensors = value.bit1
    }

    companion object : HasDefaultKeyMapping {

        override fun populateWithDefault(keyMapping: KeyMapping) {
            keyMapping.customKey(HIT_BODY, KeyboardKeys.NUMBER_5)
            keyMapping.customKey(HOOK_LEFT, KeyboardKeys.NUMBER_7)
            keyMapping.customKey(HOOK_RIGHT, KeyboardKeys.NUMBER_9)
            keyMapping.customKey(JAB_LEFT, KeyboardKeys.NUMBER_1)
            keyMapping.customKey(JAB_RIGHT, KeyboardKeys.NUMBER_3)
            keyMapping.customKey(MOVE_LEFT, KeyboardKeys.NUMBER_4)
            keyMapping.customKey(MOVE_RIGHT, KeyboardKeys.NUMBER_6)
            keyMapping.customKey(STRAIGHT, KeyboardKeys.NUMBER_8)
        }
    }
}
