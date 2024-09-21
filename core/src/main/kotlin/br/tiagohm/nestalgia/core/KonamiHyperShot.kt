package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.KONAMI_HYPER_SHOT
import br.tiagohm.nestalgia.core.KonamiHyperShot.Button.*

// https://www.nesdev.org/wiki/Konami_Hyper_Shot

class KonamiHyperShot(
    console: Console,
    private val keyMapping: KeyMapping,
) : ControlDevice(console, KONAMI_HYPER_SHOT, EXP_DEVICE_PORT) {

    enum class Button : ControllerButton, HasCustomKey {
        RUN_P1,
        JUMP_P1,
        RUN_P2,
        JUMP_P2;

        override val bit = ordinal

        override val keyIndex = 3 + ordinal
    }

    @Volatile private var enableP1 = true
    @Volatile private var enableP2 = true

    private val keys = Button.entries.map(keyMapping::customKey).toTypedArray()

    override fun setStateFromInput() {
        Button.entries.forEach { setPressedState(it, keys[it.ordinal]) }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr == 0x4017) {
            var output = 0

            if (enableP1) {
                output = if (isPressed(JUMP_P1)) 0x02 else 0
                output = output or if (isPressed(RUN_P1)) 0x04 else 0
            }

            if (enableP2) {
                output = output or if (isPressed(JUMP_P2)) 0x08 else 0
                output = output or if (isPressed(RUN_P2)) 0x10 else 0
            }

            output
        } else {
            0
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        enableP2 = !value.bit1
        enableP1 = !value.bit2
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("enableP1", enableP1)
        s.write("enableP2", enableP2)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        enableP1 = s.readBoolean("enableP1")
        enableP2 = s.readBoolean("enableP2")
    }

    companion object : HasDefaultKeyMapping {

        override fun populateWithDefault(keyMapping: KeyMapping) {
            keyMapping.customKey(RUN_P1, KeyboardKeys.A)
            keyMapping.customKey(JUMP_P1, KeyboardKeys.S)
            keyMapping.customKey(RUN_P2, KeyboardKeys.K)
            keyMapping.customKey(JUMP_P2, KeyboardKeys.L)
        }
    }
}
