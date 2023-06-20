package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.ExcitingBoxingButton.*

class ExcitingBoxingController(
    console: Console,
    private val keyMapping: KeyMapping,
) : ControlDevice(console, EXCITING_BOXING, EXP_DEVICE_PORT) {

    private var selectedSensors = false
    private val keys = Array(8) { keyMapping.customKey(ExcitingBoxingButton.entries[it]) }

    override fun setStateFromInput() {
        ExcitingBoxingButton.entries.forEach { setPressedState(it, keys[it.ordinal]) }
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
}
