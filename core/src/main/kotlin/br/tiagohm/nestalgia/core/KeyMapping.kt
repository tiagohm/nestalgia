package br.tiagohm.nestalgia.core

import java.awt.event.KeyEvent

data class KeyMapping(
    @JvmField var a: Int = 0,
    @JvmField var b: Int = 0,
    @JvmField var up: Int = 0,
    @JvmField var down: Int = 0,
    @JvmField var left: Int = 0,
    @JvmField var right: Int = 0,
    @JvmField var start: Int = 0,
    @JvmField var select: Int = 0,
    @JvmField var microphone: Int = 0,
) : Snapshotable {

    fun key(button: ControllerButton): Int {
        return when (button) {
            StandardControllerButton.UP -> up
            StandardControllerButton.DOWN -> down
            StandardControllerButton.LEFT -> left
            StandardControllerButton.RIGHT -> right
            StandardControllerButton.START -> start
            StandardControllerButton.SELECT -> select
            StandardControllerButton.B -> b
            StandardControllerButton.A -> a
            StandardControllerButton.MICROPHONE -> microphone
            else -> 0
        }
    }

    fun isEmpty() = a == 0 &&
        b == 0 &&
        up == 0 &&
        down == 0 &&
        left == 0 &&
        right == 0 &&
        start == 0 &&
        select == 0 &&
        microphone == 0

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
    }

    override fun saveState(s: Snapshot) {
        s.write("a", a)
        s.write("b", b)
        s.write("up", up)
        s.write("down", down)
        s.write("left", left)
        s.write("right", right)
        s.write("select", select)
        s.write("start", start)
        s.write("microphone", microphone)
    }

    override fun restoreState(s: Snapshot) {
        a = s.readInt("a")
        b = s.readInt("b")
        up = s.readInt("up")
        down = s.readInt("down")
        left = s.readInt("left")
        right = s.readInt("right")
        start = s.readInt("start")
        select = s.readInt("select")
        microphone = s.readInt("microphone")
    }

    companion object {

        @JvmStatic
        fun defaultKeys() = KeyMapping(
            KeyEvent.VK_A, KeyEvent.VK_S, // A B
            KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, // UP DOWN LEFT RIGHT
            KeyEvent.VK_ENTER, KeyEvent.VK_SPACE, // START SELECT
        )
    }
}
