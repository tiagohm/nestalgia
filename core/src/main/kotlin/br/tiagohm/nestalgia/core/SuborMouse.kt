package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.SUBOR_MOUSE
import kotlin.math.abs
import kotlin.math.min

// https://www.nesdev.org/wiki/Subor_Mouse

class SuborMouse(console: Console, port: Int, keyMapping: KeyMapping) : ControlDevice(console, SUBOR_MOUSE, port) {

    enum class Button : ControllerButton, HasCustomKey {
        LEFT,
        RIGHT;

        override val bit = ordinal
        override val keyIndex = 59 + ordinal
    }

    @Volatile private var stateBuffer = 0
    private val packetBytes = IntArray(3)
    @Volatile private var packetPos = 0
    @Volatile private var packetSize = 1

    @Volatile private var x = 0
    @Volatile private var y = 0
    @Volatile private var dx = 0
    @Volatile private var dy = 0

    private val keys = Button.entries.map(keyMapping::customKey).toTypedArray()

    override fun setStateFromInput() {
        Button.entries.forEach { setPressedState(it, keys[it.ordinal]) }

        x = console.keyManager.mouseX
        y = console.keyManager.mouseY
        dx = console.keyManager.mouseDx
        dy = console.keyManager.mouseDy
    }

    override fun refreshStateBuffer() {
        if (packetPos < packetSize - 1) {
            // 3-byte packet is not done yet, move to next byte
            packetPos++
            stateBuffer = packetBytes[packetPos]
            return
        }

        val upFlag = dy < 0
        val leftFlag = dx < 0

        dx = min(abs(dx), 31)
        dy = min(abs(dy), 31)

        if (dx <= 1 && dy <= 1) {
            packetBytes[0] = (if (isPressed(Button.LEFT)) 0x80 else 0) or
                (if (isPressed(Button.RIGHT)) 0x40 else 0) or
                (if (leftFlag && dx != 0) 0x30 else if (dx != 0) 0x10 else 0) or
                (if (upFlag && dy != 0) 0x0C else if (dy != 0) 0x04 else 0)
            packetBytes[1] = 0
            packetBytes[2] = 0

            packetSize = 1
        } else {
            // 3-byte packet
            packetBytes[0] = (if (isPressed(Button.LEFT)) 0x80 else 0) or
                (if (isPressed(Button.RIGHT)) 0x40 else 0) or
                (if (leftFlag) 0x20 else 0) or (dx and 0x10) or
                (if (upFlag) 0x08 else 0) or (dy and 0x10 shr 2) or 0x01

            packetBytes[1] = (dx and 0x0F shl 2) or 0x02
            packetBytes[2] = (dy and 0x0F shl 2) or 0x03

            packetSize = 3
        }

        packetPos = 0
        stateBuffer = packetBytes[0]

        if (dx > 31) dx -= 31
        else if (dx < -31) dx += 31

        if (dy > 31) dy -= 31
        else if (dy < -31) dy += 31
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        x = 0
        y = 0
        dx = 0
        dy = 0
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        var output = 0

        if ((addr == 0x4016 && !port.bit0) || (addr == 0x4017 && port.bit0)) {
            strobeOnRead()

            output = stateBuffer and 0x80 shr 7

            if (port >= 2) {
                output = output shl 1
            }

            stateBuffer = stateBuffer shl 1
        }

        return output
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobeOnWrite(value)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("stateBuffer", stateBuffer)
        s.write("packetBytes", packetBytes)
        s.write("packetPos", packetPos)
        s.write("packetSize", packetSize)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        stateBuffer = s.readInt("stateBuffer")
        s.readIntArray("packetBytes", packetBytes)
        packetPos = s.readInt("packetPos")
        packetSize = s.readInt("packetSize")
    }

    companion object : HasDefaultKeyMapping {

        override fun populateWithDefault(keyMapping: KeyMapping) {
            keyMapping.customKey(Button.LEFT, MouseButton.LEFT)
            keyMapping.customKey(Button.RIGHT, MouseButton.RIGHT)
        }
    }
}
