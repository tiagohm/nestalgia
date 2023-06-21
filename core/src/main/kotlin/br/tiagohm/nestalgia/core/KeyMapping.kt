package br.tiagohm.nestalgia.core

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
    @JvmField var turboA: Key = Key.UNDEFINED,
    @JvmField var turboB: Key = Key.UNDEFINED,
    @JvmField val customKeys: Array<Key> = Array(100) { Key.UNDEFINED },
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
        else -> customKey(button)
    }

    fun key(button: ControllerButton, key: Key) {
        when (button) {
            StandardControllerButton.UP -> up = key
            StandardControllerButton.DOWN -> down = key
            StandardControllerButton.LEFT -> left = key
            StandardControllerButton.RIGHT -> right = key
            StandardControllerButton.START -> start = key
            StandardControllerButton.SELECT -> select = key
            StandardControllerButton.B -> b = key
            StandardControllerButton.A -> a = key
            StandardControllerButton.MICROPHONE -> microphone = key
            StandardControllerButton.TURBO_B -> turboB = key
            StandardControllerButton.TURBO_A -> turboA = key
            else -> customKey(button, key)
        }
    }

    fun customKey(button: ControllerButton): Key {
        return if (button.isCustomKey) customKeys[button.bit] else Key.UNDEFINED
    }

    fun customKey(button: ControllerButton, key: Key) {
        if (button.bit in customKeys.indices) {
            customKeys[button.bit] = key
        }
    }

    fun isEmpty() = a.code == 0 &&
        b.code == 0 &&
        up.code == 0 &&
        down.code == 0 &&
        left.code == 0 &&
        right.code == 0 &&
        start.code == 0 &&
        select.code == 0 &&
        microphone.code == 0 &&
        customKeys.all { it.code == 0 }

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
        customKeys.copyInto(keyMapping.customKeys)
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
        customKeys.fill(Key.UNDEFINED)
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
        s.write("turboA", turboA.code)
        s.write("turboB", turboB.code)
        s.write("customKeys", customKeys)
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
        turboA = Key.of(s.readInt("turboA"))
        turboB = Key.of(s.readInt("turboB"))
        s.readArray("customKeys", customKeys)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyMapping

        if (a != other.a) return false
        if (b != other.b) return false
        if (up != other.up) return false
        if (down != other.down) return false
        if (left != other.left) return false
        if (right != other.right) return false
        if (start != other.start) return false
        if (select != other.select) return false
        if (microphone != other.microphone) return false
        return customKeys.contentEquals(other.customKeys)
    }

    override fun hashCode(): Int {
        var result = a.hashCode()
        result = 31 * result + b.hashCode()
        result = 31 * result + up.hashCode()
        result = 31 * result + down.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + select.hashCode()
        result = 31 * result + microphone.hashCode()
        result = 31 * result + customKeys.contentHashCode()
        return result
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
