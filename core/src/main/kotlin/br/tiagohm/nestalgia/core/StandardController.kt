package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/Standard_controller

class StandardController(console: Console, port: Int) : ControlDevice(console, port) {

    private val keys = console.settings.controllerKeys(port)

    private var microphoneEnabled = port == 1 && console.settings.consoleType == ConsoleType.FAMICOM
    private val turboSpeed = 2 // 0..4
    private val turboFreq = 1 shl (4 - turboSpeed) and 0xFF

    @Volatile private var stateBuffer = 0

    private val value
        get() = (if (isPressed(StandardControllerButton.A)) 0x01 else 0x00) or
            (if (isPressed(StandardControllerButton.B)) 0x02 else 0x00) or
            (if (isPressed(StandardControllerButton.SELECT)) 0x04 else 0x00) or
            (if (isPressed(StandardControllerButton.START)) 0x08 else 0x00) or
            (if (isPressed(StandardControllerButton.UP)) 0x10 else 0x00) or
            (if (isPressed(StandardControllerButton.DOWN)) 0x20 else 0x00) or
            (if (isPressed(StandardControllerButton.LEFT)) 0x40 else 0x00) or
            (if (isPressed(StandardControllerButton.RIGHT)) 0x80 else 0x00)

    val isTurboOn
        get() = (console.frameCount % turboFreq) < (turboFreq / 2)

    private val isMicrophoneEnabled
        get() = microphoneEnabled && console.frameCount % 3 == 0

    override fun setStateFromInput() {
        pressedStateFromKeys(StandardControllerButton.A)
        pressedStateFromKeys(StandardControllerButton.B)
        pressedStateFromKeys(StandardControllerButton.START)
        pressedStateFromKeys(StandardControllerButton.SELECT)
        pressedStateFromKeys(StandardControllerButton.UP)
        pressedStateFromKeys(StandardControllerButton.DOWN)
        pressedStateFromKeys(StandardControllerButton.LEFT)
        pressedStateFromKeys(StandardControllerButton.RIGHT)

        if (isTurboOn) {
            pressedStateFromKeys(StandardControllerButton.TURBO_A)
            pressedStateFromKeys(StandardControllerButton.TURBO_B)
        }

        if (isMicrophoneEnabled) {
            pressedStateFromKeys(StandardControllerButton.MICROPHONE)
        }

        if (!console.settings.flag(EmulationFlag.ALLOW_INVALID_INPUT)) {
            // If both U+D or L+R are pressed at the same time, act as if neither is pressed
            if (isPressed(StandardControllerButton.UP) && isPressed(StandardControllerButton.DOWN)) {
                setBit(StandardControllerButton.UP)
                setBit(StandardControllerButton.DOWN)
            }
            if (isPressed(StandardControllerButton.LEFT) && isPressed(StandardControllerButton.RIGHT)) {
                setBit(StandardControllerButton.LEFT)
                setBit(StandardControllerButton.RIGHT)
            }
        }
    }

    private fun pressedStateFromKeys(button: ControllerButton) {
        setPressedState(button, keys.key(button))
    }

    override fun refreshStateBuffer() {
        stateBuffer = value
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        if (port >= 2 && console.dualSystem) {
            // Ignore P3/P4 controllers for VS DualSystem - those are used by the slave CPU
            return 0
        }

        var output = 0

        if (isCurrentPort(addr)) {
            strobeOnRead()

            output = stateBuffer and 0x01

            if (port >= 2 && console.settings.consoleType == ConsoleType.FAMICOM) {
                // Famicom outputs P3 & P4 on bit 1
                output = output shl 1
            }

            stateBuffer = stateBuffer shr 1

            // All subsequent reads will return D=1 on an authentic controller
            // but may return D=0 on third party controllers.
            stateBuffer = stateBuffer or 0x80
        }

        if (addr == 0x4016 && isPressed(StandardControllerButton.MICROPHONE)) {
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
