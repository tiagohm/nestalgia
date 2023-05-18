package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
class KeyMapping(
    val a: Int,
    val b: Int,
    val up: Int,
    val down: Int,
    val left: Int,
    val right: Int,
    val start: Int,
    val select: Int,
    val microphone: Int = 0,
) {

    inline fun getKey(button: Button): Int {
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

    fun toSnapshot(): Snapshot {
        return Snapshot().also { s ->
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

        return true
    }

    override fun hashCode(): Int {
        var result = a
        result = 31 * result + b
        result = 31 * result + up
        result = 31 * result + down
        result = 31 * result + left
        result = 31 * result + right
        result = 31 * result + start
        result = 31 * result + select
        result = 31 * result + microphone
        return result
    }

    companion object {

        @JvmStatic val NONE = KeyMapping(0, 0, 0, 0, 0, 0, 0, 0, 0)

        @JvmStatic
        fun load(s: Snapshot) = KeyMapping(
            s.readInt("a") ?: 0,
            s.readInt("b") ?: 0,
            s.readInt("up") ?: 0,
            s.readInt("down") ?: 0,
            s.readInt("left") ?: 0,
            s.readInt("right") ?: 0,
            s.readInt("start") ?: 0,
            s.readInt("select") ?: 0,
            s.readInt("microphone") ?: 0,
        )
    }
}
