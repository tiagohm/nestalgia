package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.OEKA_KIDS_TABLET
import kotlin.math.max

// https://www.nesdev.org/wiki/Oeka_Kids_tablet

class OekaKidsTablet(console: Console) : ControlDevice(console, OEKA_KIDS_TABLET, EXP_DEVICE_PORT) {

    enum class Button : ControllerButton {
        CLICK,
        TOUCH;

        override val bit = ordinal
    }

    @Volatile private var shift = false
    @Volatile private var stateBuffer = 0

    @Volatile private var x = 0
    @Volatile private var y = 0

    override fun setStateFromInput() {
        if (console.settings.isInputEnabled) {
            x = console.keyManager.mouseX
            y = console.keyManager.mouseY

            setPressedState(Button.CLICK, MouseButton.LEFT)

            if (y >= 48 || console.keyManager.isKeyPressed(MouseButton.LEFT)) {
                setBit(Button.TOUCH)
            } else {
                clearBit(Button.TOUCH)
            }
        }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        if (addr == 0x4017) {
            if (strobe) {
                return if (shift) {
                    if (stateBuffer and 0x40000 != 0) 0x00 else 0x08
                } else {
                    0x04
                }
            }
        }

        return 0
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobe = value.bit0

        if (strobe) {
            val shifted = value.bit1

            if (!shift && shifted) {
                stateBuffer = stateBuffer shl 1
            }

            shift = shifted
        } else {
            val xp = (max(0, x + 8) / 256f * 240).toInt()
            val yp = (max(0, y - 14) / 240f * 256).toInt()

            stateBuffer = (xp shl 10) or (yp shl 2) or (if (isPressed(Button.TOUCH)) 0x02 else 0x00) or (if (isPressed(Button.CLICK)) 0x01 else 0x00)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("shift", shift)
        s.write("stateBuffer", stateBuffer)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        shift = s.readBoolean("shift")
        stateBuffer = s.readInt("stateBuffer")
    }
}
