package br.tiagohm.nestalgia.core

import java.io.*

class Snapshot private constructor(private val data: MutableMap<String, Any>) : Serializable {

    constructor(capacity: Int = 32) : this(HashMap(capacity))

    val size
        get() = data.size

    val keys
        get() = data.keys.toList()

    operator fun contains(key: String): Boolean {
        return key in data
    }

    operator fun get(key: String): Any? {
        return data[key]
    }

    fun write(key: String, value: Boolean) {
        data[key] = value
    }

    fun write(key: String, value: Int) {
        data[key] = value
    }

    fun write(key: String, value: Long) {
        data[key] = value
    }

    fun write(key: String, value: Snapshotable) {
        val snapshot = Snapshot()
        value.saveState(snapshot)
        write(key, snapshot)
    }

    fun write(key: String, value: Snapshot) {
        data[key] = value
    }

    fun write(key: String, value: Enum<*>) {
        data[key] = value
    }

    fun write(key: String, value: BooleanArray) {
        data[key] = value
    }

    fun write(key: String, value: ShortArray) {
        data[key] = value
    }

    fun write(key: String, value: IntArray) {
        data[key] = value
    }

    fun write(key: String, value: Array<out Serializable>) {
        data[key] = value
    }

    fun write(key: String, value: String) {
        data[key] = value
    }

    fun readBoolean(key: String, value: Boolean = false): Boolean {
        return data[key] as? Boolean ?: value
    }

    fun readInt(key: String, value: Int = 0): Int {
        return data[key] as? Int ?: value
    }

    fun readLong(key: String, value: Long = 0L): Long {
        return data[key] as? Long ?: value
    }

    fun readSnapshot(key: String): Snapshot? {
        return data[key] as? Snapshot
    }

    fun readSnapshotable(key: String, output: Snapshotable, onNull: Runnable? = null): Boolean {
        val snapshot = readSnapshot(key) ?: return run { onNull?.run(); false }
        output.restoreState(snapshot)
        return true
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Enum<T>> readEnum(key: String, value: T): T {
        return data[key] as? T ?: value
    }

    fun readBooleanArray(key: String): BooleanArray? {
        return data[key] as? BooleanArray
    }

    fun readBooleanArray(key: String, output: BooleanArray): BooleanArray? {
        return readBooleanArray(key)?.copyInto(output)
    }

    fun readBooleanArrayOrFill(key: String, output: BooleanArray, value: Boolean) {
        readBooleanArray(key)?.copyInto(output) ?: output.fill(value)
    }

    fun readShortArray(key: String): ShortArray? {
        return data[key] as? ShortArray
    }

    fun readIntArray(key: String): IntArray? {
        return data[key] as? IntArray
    }

    fun readIntArray(key: String, output: IntArray): IntArray? {
        return readIntArray(key)?.copyInto(output)
    }

    fun readIntArrayOrFill(key: String, output: IntArray, value: Int) {
        readIntArray(key)?.copyInto(output) ?: output.fill(value)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> readArray(key: String): Array<out T>? {
        return data[key] as? Array<out T>
    }

    fun <T> readArray(key: String, output: Array<T>): Array<out T>? {
        return readArray<T>(key)?.copyInto(output)
    }

    fun readString(key: String, value: String = ""): String {
        return data[key] as? String ?: value
    }

    fun writeTo(sink: OutputStream) {
        val stream = ObjectOutputStream(sink)
        stream.writeObject(this)
    }

    fun bytes(size: Int = 1024 * 32): ByteArray {
        val sink = ByteArrayOutputStream(size)
        writeTo(sink)
        return sink.toByteArray()
    }

    companion object {

        @JvmStatic private val serialVersionUID = 1L

        fun from(source: InputStream): Snapshot {
            val stream = ObjectInputStream(source)
            return stream.readObject() as Snapshot
        }

        fun from(source: ByteArray): Snapshot {
            return from(ByteArrayInputStream(source))
        }
    }
}
