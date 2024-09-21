package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.StandardController.Button.*

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
    @JvmField val customKeys: Array<Key> = Array(256) { Key.UNDEFINED },
) : Snapshotable, Resetable {

    fun key(button: ControllerButton) = when (button) {
        UP -> up
        DOWN -> down
        LEFT -> left
        RIGHT -> right
        START -> start
        SELECT -> select
        B -> b
        A -> a
        MICROPHONE -> microphone
        TURBO_A -> turboA
        TURBO_B -> turboB
        else -> customKey(button)
    }

    fun key(button: ControllerButton, key: Key) {
        when (button) {
            UP -> up = key
            DOWN -> down = key
            LEFT -> left = key
            RIGHT -> right = key
            START -> start = key
            SELECT -> select = key
            B -> b = key
            A -> a = key
            MICROPHONE -> microphone = key
            TURBO_B -> turboB = key
            TURBO_A -> turboA = key
            else -> customKey(button, key)
        }
    }

    fun customKey(button: ControllerButton): Key {
        return if (button is HasCustomKey) customKey(button.keyIndex) else Key.UNDEFINED
    }

    fun customKey(index: Int): Key {
        return if (index in customKeys.indices) customKeys[index] else Key.UNDEFINED
    }

    fun customKey(button: ControllerButton, key: Key) {
        if (button is HasCustomKey) {
            customKey(button.keyIndex, key)
        }
    }

    fun customKey(index: Int, key: Key) {
        if (index in customKeys.indices) {
            customKeys[index] = key
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

    fun copyInto(keyMapping: KeyMapping) {
        keyMapping.a = a
        keyMapping.b = b
        keyMapping.up = up
        keyMapping.down = down
        keyMapping.left = left
        keyMapping.right = right
        keyMapping.select = select
        keyMapping.start = start
        keyMapping.microphone = microphone
        keyMapping.turboA = turboA
        keyMapping.turboB = turboB
        customKeys.copyInto(keyMapping.customKeys)
    }

    fun copyIntoIfUndefined(keyMapping: KeyMapping): Boolean {
        var modified = false

        if (keyMapping.a == Key.UNDEFINED) keyMapping.a = a.also { modified = true }
        if (keyMapping.b == Key.UNDEFINED) keyMapping.b = b.also { modified = true }
        if (keyMapping.up == Key.UNDEFINED) keyMapping.up = up.also { modified = true }
        if (keyMapping.down == Key.UNDEFINED) keyMapping.down = down.also { modified = true }
        if (keyMapping.left == Key.UNDEFINED) keyMapping.left = left.also { modified = true }
        if (keyMapping.right == Key.UNDEFINED) keyMapping.right = right.also { modified = true }
        if (keyMapping.select == Key.UNDEFINED) keyMapping.select = select.also { modified = true }
        if (keyMapping.start == Key.UNDEFINED) keyMapping.start = start.also { modified = true }
        if (keyMapping.microphone == Key.UNDEFINED) keyMapping.microphone = microphone.also { modified = true }
        if (keyMapping.turboA == Key.UNDEFINED) keyMapping.turboA = turboA.also { modified = true }
        if (keyMapping.turboB == Key.UNDEFINED) keyMapping.turboB = turboB.also { modified = true }

        for (i in customKeys.indices) {
            if (keyMapping.customKeys[i] == Key.UNDEFINED) {
                keyMapping.customKeys[i] = customKeys[i]
                modified = true
            }
        }

        return modified
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
}
