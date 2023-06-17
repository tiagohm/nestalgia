package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MouseButton.*

data class KeyMapping(
    @JvmField var a: Key = Key.UNDEFINED,
    @JvmField var b: Key = Key.UNDEFINED,
    @JvmField var up: Key = Key.UNDEFINED,
    @JvmField var down: Key = Key.UNDEFINED,
    @JvmField var left: Key = Key.UNDEFINED,
    @JvmField var right: Key = Key.UNDEFINED,
    @JvmField var start: Key = Key.UNDEFINED,
    @JvmField var select: Key = Key.UNDEFINED,
    @JvmField var microphone: Key = Key.UNDEFINED,
    @JvmField var zapperFire: MouseButton = LEFT,
    @JvmField var zapperAimOffscreen: MouseButton = RIGHT,
    @JvmField var arkanoidFire: MouseButton = LEFT,
) : Snapshotable, Resetable {

    fun key(button: ControllerButton) = when (button) {
        StandardControllerButton.UP -> up
        StandardControllerButton.DOWN -> down
        StandardControllerButton.LEFT -> left
        StandardControllerButton.RIGHT -> right
        StandardControllerButton.START -> start
        StandardControllerButton.SELECT -> select
        StandardControllerButton.B -> b
        StandardControllerButton.A -> a
        StandardControllerButton.MICROPHONE -> microphone
        ZapperButton.FIRE -> zapperFire
        ZapperButton.AIM_OFFSCREEN -> zapperAimOffscreen
        ArkanoidButton.FIRE -> zapperFire
        else -> Key.UNDEFINED
    }

    fun isEmpty() = a.code == 0 &&
        b.code == 0 &&
        up.code == 0 &&
        down.code == 0 &&
        left.code == 0 &&
        right.code == 0 &&
        start.code == 0 &&
        select.code == 0 &&
        microphone.code == 0

    fun copyTo(keyMapping: KeyMapping) {
        keyMapping.a = a
        keyMapping.b = b
        keyMapping.up = up
        keyMapping.down = down
        keyMapping.left = left
        keyMapping.right = right
        keyMapping.select = select
        keyMapping.start = start
        keyMapping.microphone = microphone
        keyMapping.zapperFire = zapperFire
        keyMapping.zapperAimOffscreen = zapperAimOffscreen
    }

    override fun reset(softReset: Boolean) {
        a = Key.UNDEFINED
        b = Key.UNDEFINED
        up = Key.UNDEFINED
        down = Key.UNDEFINED
        left = Key.UNDEFINED
        right = Key.UNDEFINED
        select = Key.UNDEFINED
        start = Key.UNDEFINED
        microphone = Key.UNDEFINED
        zapperFire = LEFT
        zapperAimOffscreen = RIGHT
    }

    override fun saveState(s: Snapshot) {
        s.write("a", a.code)
        s.write("b", b.code)
        s.write("up", up.code)
        s.write("down", down.code)
        s.write("left", left.code)
        s.write("right", right.code)
        s.write("select", select.code)
        s.write("start", start.code)
        s.write("microphone", microphone.code)
        s.write("zapperFire", zapperFire.code)
        s.write("zapperAimOffscreen", zapperAimOffscreen.code)
    }

    override fun restoreState(s: Snapshot) {
        a = Key.of(s.readInt("a"))
        b = Key.of(s.readInt("b"))
        up = Key.of(s.readInt("up"))
        down = Key.of(s.readInt("down"))
        left = Key.of(s.readInt("left"))
        right = Key.of(s.readInt("right"))
        start = Key.of(s.readInt("start"))
        select = Key.of(s.readInt("select"))
        microphone = Key.of(s.readInt("microphone"))
    }

    companion object {

        @JvmStatic
        fun wasd() = KeyMapping(
            KeyboardKeys.E, KeyboardKeys.Q,
            KeyboardKeys.W, KeyboardKeys.S,
            KeyboardKeys.A, KeyboardKeys.D,
            KeyboardKeys.X, KeyboardKeys.Z,
        )

        @JvmStatic
        fun arrowKeys() = KeyMapping(
            KeyboardKeys.L, KeyboardKeys.K,
            KeyboardKeys.UP, KeyboardKeys.DOWN,
            KeyboardKeys.LEFT, KeyboardKeys.RIGHT,
            KeyboardKeys.ENTER, KeyboardKeys.SPACE,
        )
    }
}
