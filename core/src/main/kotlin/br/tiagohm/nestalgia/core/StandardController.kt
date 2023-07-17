package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.StandardController.Button.*

// https://wiki.nesdev.com/w/index.php/Standard_controller

open class StandardController(
    console: Console, type: ControllerType, port: Int,
    private val keyMapping: KeyMapping,
) : ControlDevice(console, type, port) {

    enum class Button(override val bit: Int) : ControllerButton {
        UP(0),
        DOWN(1),
        LEFT(2),
        RIGHT(3),
        START(4),
        SELECT(5),
        B(6),
        A(7),
        MICROPHONE(8),
        TURBO_B(6),
        TURBO_A(7),
    }

    private var microphoneEnabled = port == 1 && type == FAMICOM_CONTROLLER_P2
    private val turboSpeed = 2 // 0..4
    private val turboFreq = 1 shl (4 - turboSpeed) and 0xFF

    protected var stateBuffer = 0
        private set

    protected val value
        get() = (if (isPressed(A)) 0x01 else 0x00) or
            (if (isPressed(B)) 0x02 else 0x00) or
            (if (isPressed(SELECT)) 0x04 else 0x00) or
            (if (isPressed(START)) 0x08 else 0x00) or
            (if (isPressed(UP)) 0x10 else 0x00) or
            (if (isPressed(DOWN)) 0x20 else 0x00) or
            (if (isPressed(LEFT)) 0x40 else 0x00) or
            (if (isPressed(RIGHT)) 0x80 else 0x00)

    val isTurboOn
        get() = (console.frameCount % turboFreq) < (turboFreq / 2)

    private val isMicrophoneEnabled
        get() = microphoneEnabled && console.frameCount % 3 == 0

    override fun setStateFromInput() {
        pressedStateFromKeys(A)
        pressedStateFromKeys(B)
        pressedStateFromKeys(START)
        pressedStateFromKeys(SELECT)
        pressedStateFromKeys(UP)
        pressedStateFromKeys(DOWN)
        pressedStateFromKeys(LEFT)
        pressedStateFromKeys(RIGHT)

        if (isTurboOn) {
            pressedStateFromKeys(TURBO_A)
            pressedStateFromKeys(TURBO_B)
        }

        if (isMicrophoneEnabled) {
            pressedStateFromKeys(MICROPHONE)
        }

        if (!console.settings.flag(EmulationFlag.ALLOW_INVALID_INPUT)) {
            // If both U+D or L+R are pressed at the same time, act as if neither is pressed
            if (isPressed(UP) && isPressed(DOWN)) {
                setBit(UP)
                setBit(DOWN)
            }
            if (isPressed(LEFT) && isPressed(RIGHT)) {
                setBit(LEFT)
                setBit(RIGHT)
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun pressedStateFromKeys(button: ControllerButton) {
        setPressedState(button, keyMapping.key(button))
    }

    override fun refreshStateBuffer() {
        stateBuffer = value
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        var output = 0

        if (isCurrentPort(addr)) {
            strobeOnRead()

            output = stateBuffer and 0x01

            stateBuffer = stateBuffer shr 1

            // All subsequent reads will return D=1 on an authentic controller
            // but may return D=0 on third party controllers.
            stateBuffer = stateBuffer or 0x80
        }

        if (addr == 0x4016 && isPressed(MICROPHONE)) {
            output = output or 0x04
        }

        return output
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobeOnWrite(value)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("stateBuffer", stateBuffer)
        s.write("microphoneEnabled", microphoneEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        stateBuffer = s.readInt("stateBuffer")
        microphoneEnabled = s.readBoolean("microphoneEnabled")
    }
}
