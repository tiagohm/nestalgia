package br.tiagohm.nestalgia.core

import okio.Buffer
import java.io.IOException
import java.io.OutputStream

@ExperimentalUnsignedTypes
open class Snapshot(buffer: Buffer = Buffer()) {

    constructor(data: ByteArray) : this(Buffer().also { it.write(data) })

    enum class DataType {
        BOOLEAN,
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        ENUM,
        STREAM,
        ASCII,
        UTF8,
    }

    data class KeyInfo(
        val name: String,
        val type: DataType,
        val isUnsigned: Boolean,
        val isArray: Boolean,
    )

    private val buffer = ByteStream(buffer)

    @PublishedApi
    internal val data = LinkedHashMap<String, Any>()

    val size: Int
        get() = buffer.size

    val bytes: ByteArray
        get() = buffer.bytes

    val stream: OutputStream
        get() = buffer.stream

    inline val isEmpty: Boolean
        get() = size == 0

    inline val isNotEmpty: Boolean
        get() = !isEmpty

    val keys: Set<String>
        get() = data.keys

    var isLoaded = false
        private set

    private fun writeKey(
        key: String,
        type: DataType,
        isUnsigned: Boolean = false,
        isArray: Boolean = false,
    ) {
        // [3-2:type][1: signed flag][0:array flag]
        val data = (type.ordinal shl 2) or
                (if (isUnsigned) 0x2 else 0) or
                (if (isArray) 0x1 else 0)
        buffer.write(data)
        buffer.writeAscii(key)
    }

    // Write

    fun write(key: String, b: Boolean) {
        writeKey(key, DataType.BOOLEAN)
        buffer.write(b)
    }

    fun write(key: String, b: UByte) {
        writeKey(key, DataType.BYTE, isUnsigned = true)
        buffer.write(b)
    }

    fun write(key: String, b: Byte) {
        writeKey(key, DataType.BYTE)
        buffer.write(b)
    }

    fun write(key: String, s: UShort) {
        writeKey(key, DataType.SHORT, isUnsigned = true)
        buffer.write(s)
    }

    fun write(key: String, s: Short) {
        writeKey(key, DataType.SHORT)
        buffer.write(s)
    }

    fun write(key: String, i: UInt) {
        writeKey(key, DataType.INT, isUnsigned = true)
        buffer.write(i)
    }

    fun write(key: String, i: Int) {
        writeKey(key, DataType.INT)
        buffer.write(i)
    }

    fun write(key: String, l: ULong) {
        writeKey(key, DataType.LONG, isUnsigned = true)
        buffer.write(l)
    }

    fun write(key: String, l: Long) {
        writeKey(key, DataType.LONG)
        buffer.write(l)
    }

    fun write(key: String, f: Float) {
        writeKey(key, DataType.FLOAT)
        buffer.write(f)
    }

    fun write(key: String, d: Double) {
        writeKey(key, DataType.DOUBLE)
        buffer.write(d)
    }

    fun write(key: String, e: Enum<*>) {
        writeKey(key, DataType.ENUM)
        buffer.write(e)
    }

    fun write(key: String, a: Array<out Enum<*>>) {
        val data = IntArray(a.size) { i -> a[i].ordinal }
        write(key, data)
    }

    fun write(key: String, a: BooleanArray) {
        writeKey(key, DataType.BOOLEAN, isArray = true)
        buffer.write(a.size)
        buffer.write(a)
    }

    fun write(key: String, a: ByteArray) {
        writeKey(key, DataType.BYTE, isArray = true)
        buffer.write(a.size)
        buffer.write(a)
    }

    fun write(key: String, a: UByteArray) {
        writeKey(key, DataType.BYTE, isUnsigned = true, isArray = true)
        buffer.write(a.size)
        buffer.write(a)
    }

    fun write(key: String, a: ShortArray) {
        writeKey(key, DataType.SHORT, isArray = true)
        buffer.write(a.size * 2)
        buffer.write(a)
    }

    fun write(key: String, a: UShortArray) {
        writeKey(key, DataType.SHORT, isUnsigned = true, isArray = true)
        buffer.write(a.size * 2)
        buffer.write(a)
    }

    fun write(key: String, a: IntArray) {
        writeKey(key, DataType.INT, isArray = true)
        buffer.write(a.size * 4)
        buffer.write(a)
    }

    fun write(key: String, a: UIntArray) {
        writeKey(key, DataType.INT, isUnsigned = true, isArray = true)
        buffer.write(a.size * 4)
        buffer.write(a)
    }

    fun write(key: String, a: LongArray) {
        writeKey(key, DataType.LONG, isArray = true)
        buffer.write(a.size * 8)
        buffer.write(a)
    }

    fun write(key: String, a: ULongArray) {
        writeKey(key, DataType.LONG, isUnsigned = true, isArray = true)
        buffer.write(a.size * 8)
        buffer.write(a)
    }

    fun write(key: String, a: FloatArray) {
        writeKey(key, DataType.FLOAT, isArray = true)
        buffer.write(a.size * 4)
        buffer.write(a)
    }

    fun write(key: String, a: DoubleArray) {
        writeKey(key, DataType.DOUBLE, isUnsigned = true, isArray = true)
        buffer.write(a.size * 8)
        buffer.write(a)
    }

    fun write(key: String, s: Snapshot) {
        if (s != this) {
            writeKey(key, DataType.STREAM)
            buffer.write(s.buffer)
        } else {
            throw IOException("Cannot itself be added")
        }
    }

    fun write(key: String, s: Snapshotable) {
        val snapshot = Snapshot()
        s.saveState(snapshot)
        write(key, snapshot)
    }

    fun writeAscii(key: String, s: String) {
        writeKey(key, DataType.ASCII)
        buffer.writeAscii(s)
    }

    fun writeUtf8(key: String, s: String) {
        writeKey(key, DataType.UTF8)
        buffer.writeUtf8(s)
    }

    // Read

    private fun readKey(): KeyInfo {
        val i = buffer.readInt()
        val isArray = i and 0x1 == 0x1
        val isUnsigned = i and 0x2 == 0x4
        val type = DataType.values()[i shr 2]
        val key = buffer.readAscii()
        return KeyInfo(key, type, isUnsigned, isArray)
    }

    fun load(): Int {
        while (!buffer.isEof) {
            val key = readKey()
            val keyName = key.name

            if (data.containsKey(keyName)) {
                System.err.println("Key $keyName was overrided")
            }

            if (key.isArray) {
                data[keyName] = buffer.readByteArray(buffer.readInt())
            } else {
                when (key.type) {
                    DataType.BOOLEAN -> data[keyName] = buffer.readByte().toLong()
                    DataType.BYTE -> data[keyName] = buffer.readByte().toLong()
                    DataType.SHORT -> data[keyName] = buffer.readShort().toLong()
                    DataType.INT -> data[keyName] = buffer.readInt().toLong()
                    DataType.LONG -> data[keyName] = buffer.readLong()
                    DataType.FLOAT -> data[keyName] = Float.fromBits(buffer.readInt())
                    DataType.DOUBLE -> data[keyName] = Double.fromBits(buffer.readLong())
                    DataType.ENUM -> data[keyName] = buffer.readInt().toLong()
                    DataType.STREAM,
                    DataType.ASCII,
                    DataType.UTF8 -> data[keyName] = buffer.readByteArray(buffer.readInt())
                }
            }
        }

        isLoaded = true

        return data.size
    }

    fun has(key: String) = data.containsKey(key)

    fun readBoolean(key: String): Boolean? {
        return data[key]?.let { it == 1L }
    }

    fun readByte(key: String): Byte? {
        return (data[key] as? Long)?.toByte()
    }

    fun readUByte(key: String): UByte? {
        return (data[key] as? Long)?.toUByte()
    }

    fun readShort(key: String): Short? {
        return (data[key] as? Long)?.toShort()
    }

    fun readUShort(key: String): UShort? {
        return (data[key] as? Long)?.toUShort()
    }

    fun readInt(key: String): Int? {
        return (data[key] as? Long)?.toInt()
    }

    fun readUInt(key: String): UInt? {
        return (data[key] as? Long)?.toUInt()
    }

    fun readLong(key: String): Long? {
        return data[key] as? Long
    }

    fun readULong(key: String): ULong? {
        return (data[key] as? Long)?.toULong()
    }

    fun readFloat(key: String): Float? {
        return (data[key] as? Number)?.toFloat()
    }

    fun readDouble(key: String): Double? {
        return (data[key] as? Number)?.toDouble()
    }

    fun readAscii(key: String): String? {
        return (data[key] as? ByteArray)?.let { String(it, Charsets.US_ASCII) }
    }

    fun readUtf8(key: String): String? {
        return (data[key] as? ByteArray)?.let { String(it) }
    }

    inline fun <reified E : Enum<E>> readEnum(key: String): E? {
        return (data[key] as? Long)?.let { enumValues<E>()[it.toInt()] }
    }

    inline fun <reified E : Enum<E>> readEnumArray(key: String): Array<out E>? {
        val a = readIntArray(key) ?: return null
        val values = enumValues<E>()
        return Array(a.size) { i -> values[a[i]] }
    }

    fun readBooleanArray(key: String): BooleanArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = BooleanArray(data.size)
        for (i in data.indices) a[i] = data[i] == BYTE_ONE
        return a
    }

    fun readByteArray(key: String): ByteArray? {
        return data[key] as? ByteArray
    }

    fun readUByteArray(key: String): UByteArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = UByteArray(data.size)
        for (i in data.indices) a[i] = data[i].toUByte()
        return a
    }

    fun readShortArray(key: String): ShortArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = ShortArray(data.size / 2)
        for (i in data.indices step 2) {
            a[i / 2] = ((data[i].toInt() and 0xFF shl 8) or (data[i + 1].toInt() and 0xFF)).toShort()
        }
        return a
    }

    fun readUShortArray(key: String): UShortArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = UShortArray(data.size / 2)
        for (i in data.indices step 2) {
            a[i / 2] = ((data[i].toInt() and 0xFF shl 8) or (data[i + 1].toInt() and 0xFF)).toUShort()
        }
        return a
    }

    fun readIntArray(key: String): IntArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = IntArray(data.size / 4)
        for (i in data.indices step 4) {
            a[i / 4] = (data[i].toInt() and 0xFF shl 24) or
                    (data[i + 1].toInt() and 0xFF shl 16) or
                    (data[i + 2].toInt() and 0xFF shl 8) or
                    (data[i + 3].toInt() and 0xFF)
        }
        return a
    }

    fun readUIntArray(key: String): UIntArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = UIntArray(data.size / 4)
        for (i in data.indices step 4) {
            a[i / 4] = ((data[i].toInt() and 0xFF shl 24) or
                    (data[i + 1].toInt() and 0xFF shl 16) or
                    (data[i + 2].toInt() and 0xFF shl 8) or
                    (data[i + 3].toInt() and 0xFF)).toUInt()
        }
        return a
    }

    fun readLongArray(key: String): LongArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = LongArray(data.size / 8)
        for (i in data.indices step 8) {
            a[i / 8] = (data[i].toLong() and 0xFF shl 56) or
                    (data[i + 1].toLong() and 0xFF shl 48) or
                    (data[i + 2].toLong() and 0xFF shl 40) or
                    (data[i + 3].toLong() and 0xFF shl 32) or
                    (data[i + 4].toLong() and 0xFF shl 24) or
                    (data[i + 5].toLong() and 0xFF shl 16) or
                    (data[i + 6].toLong() and 0xFF shl 8) or
                    (data[i + 7].toLong() and 0xFF)
        }
        return a
    }

    fun readULongArray(key: String): ULongArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = ULongArray(data.size / 8)
        for (i in data.indices step 8) {
            a[i / 8] = ((data[i].toLong() and 0xFF shl 56) or
                    (data[i + 1].toLong() and 0xFF shl 48) or
                    (data[i + 2].toLong() and 0xFF shl 40) or
                    (data[i + 3].toLong() and 0xFF shl 32) or
                    (data[i + 4].toLong() and 0xFF shl 24) or
                    (data[i + 5].toLong() and 0xFF shl 16) or
                    (data[i + 6].toLong() and 0xFF shl 8) or
                    (data[i + 7].toLong() and 0xFF)).toULong()
        }
        return a
    }

    fun readFloatArray(key: String): FloatArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = FloatArray(data.size / 4)
        for (i in data.indices step 4) {
            a[i / 4] = Float.fromBits(
                (data[i].toInt() and 0xFF shl 24) or
                        (data[i + 1].toInt() and 0xFF shl 16) or
                        (data[i + 2].toInt() and 0xFF shl 8) or
                        (data[i + 3].toInt() and 0xFF)
            )
        }
        return a
    }

    fun readDoubleArray(key: String): DoubleArray? {
        val data = (data[key] as? ByteArray) ?: return null
        val a = DoubleArray(data.size / 8)
        for (i in data.indices step 8) {
            a[i / 8] = Double.fromBits(
                (data[i].toLong() and 0xFF shl 56) or
                        (data[i + 1].toLong() and 0xFF shl 48) or
                        (data[i + 2].toLong() and 0xFF shl 40) or
                        (data[i + 3].toLong() and 0xFF shl 32) or
                        (data[i + 4].toLong() and 0xFF shl 24) or
                        (data[i + 5].toLong() and 0xFF shl 16) or
                        (data[i + 6].toLong() and 0xFF shl 8) or
                        (data[i + 7].toLong() and 0xFF)
            )
        }
        return a
    }

    fun readSnapshot(key: String): Snapshot? {
        val data = (data[key] as? ByteArray) ?: return null
        val buffer = Buffer()
        buffer.write(data)
        return Snapshot(buffer)
    }

    companion object {
        private const val BYTE_ZERO: Byte = 0
        private const val BYTE_ONE: Byte = 1
    }
}