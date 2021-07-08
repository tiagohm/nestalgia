package br.tiagohm.nestalgia.core

import okio.Buffer
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
@JvmInline
value class ByteStream(val buffer: Buffer = Buffer()) {

    inline val size: Int
        get() = buffer.size.toInt()

    inline val bytes: ByteArray
        get() = (stream as ByteArrayOutputStream).toByteArray()

    val stream: OutputStream
        get() {
            val data = ByteArrayOutputStream(size)
            buffer.copyTo(data)
            return data
        }

    inline val isEmpty: Boolean
        get() = size == 0

    inline val isNotEmpty: Boolean
        get() = !isEmpty

    inline val isEof: Boolean
        get() = buffer.exhausted()

    // Write

    fun write(b: Boolean) {
        buffer.writeByte(b.toInt())
    }

    fun write(b: UByte) {
        buffer.writeByte(b.toInt())
    }

    fun write(b: Byte) {
        buffer.writeByte(b.toInt())
    }

    fun write(s: UShort) {
        buffer.writeShort(s.toInt())
    }

    fun write(s: Short) {
        buffer.writeShort(s.toInt())
    }

    fun write(i: UInt) {
        buffer.writeInt(i.toInt())
    }

    fun write(i: Int) {
        buffer.writeInt(i)
    }

    fun write(l: ULong) {
        buffer.writeLong(l.toLong())
    }

    fun write(l: Long) {
        buffer.writeLong(l)
    }

    fun write(f: Float) {
        buffer.writeInt(f.toBits())
    }

    fun write(d: Double) {
        buffer.writeLong(d.toBits())
    }

    fun write(e: Enum<*>) {
        buffer.writeInt(e.ordinal)
    }

    fun write(a: BooleanArray) {
        a.forEach { write(it) }
    }

    fun write(a: ByteArray) {
        buffer.write(a)
    }

    fun write(a: UByteArray) {
        a.forEach { write(it) }
    }

    fun write(a: ShortArray) {
        a.forEach { write(it) }
    }

    fun write(a: UShortArray) {
        a.forEach { write(it) }
    }

    fun write(a: IntArray) {
        a.forEach { write(it) }
    }

    fun write(a: UIntArray) {
        a.forEach { write(it) }
    }

    fun write(a: LongArray) {
        a.forEach { write(it) }
    }

    fun write(a: ULongArray) {
        a.forEach { write(it) }
    }

    fun write(a: FloatArray) {
        a.forEach { write(it) }
    }

    fun write(a: DoubleArray) {
        a.forEach { write(it) }
    }

    fun write(s: ByteStream) {
        val size = s.buffer.size
        write(size.toInt())
        buffer.write(s.buffer.copy(), size)
    }

    fun writeAscii(s: String) {
        writeString(s, Charsets.US_ASCII)
    }

    fun writeUtf8(s: String) {
        writeString(s, Charsets.UTF_8)
    }

    fun writeString(s: String, charset: Charset) {
        val bytes = s.toByteArray(charset)
        buffer.writeInt(bytes.size)
        buffer.write(bytes)
    }

    fun writeLe(s: UShort) {
        buffer.writeShortLe(s.toInt())
    }

    fun writeLe(s: Short) {
        buffer.writeShortLe(s.toInt())
    }

    fun writeLe(i: UInt) {
        buffer.writeIntLe(i.toInt())
    }

    fun writeLe(i: Int) {
        buffer.writeIntLe(i)
    }

    fun writeLe(l: ULong) {
        buffer.writeLongLe(l.toLong())
    }

    fun writeLe(l: Long) {
        buffer.writeLongLe(l)
    }

    fun writeLe(f: Float) {
        buffer.writeIntLe(f.toBits())
    }

    fun writeLe(f: Double) {
        buffer.writeLongLe(f.toBits())
    }

    fun writeLe(e: Enum<*>) {
        buffer.writeIntLe(e.ordinal)
    }

    fun writeLe(a: ShortArray) {
        a.forEach { writeLe(it) }
    }

    fun writeLe(a: UShortArray) {
        a.forEach { writeLe(it) }
    }

    fun writeLe(a: IntArray) {
        a.forEach { writeLe(it) }
    }

    fun writeLe(a: UIntArray) {
        a.forEach { writeLe(it) }
    }

    fun writeLe(a: LongArray) {
        a.forEach { writeLe(it) }
    }

    fun writeLe(a: ULongArray) {
        a.forEach { writeLe(it) }
    }

    fun writeLe(a: FloatArray) {
        a.forEach { writeLe(it) }
    }

    fun writeLe(a: DoubleArray) {
        a.forEach { writeLe(it) }
    }

    // Read

    fun readBoolean(): Boolean {
        return buffer.readByte().toInt() != 0
    }

    fun readByte(): Byte {
        return buffer.readByte()
    }

    fun readUByte(): UByte {
        return buffer.readByte().toUByte()
    }

    fun readShort(): Short {
        return buffer.readShort()
    }

    fun readUShort(): UShort {
        return buffer.readShort().toUShort()
    }

    fun readShortLe(): Short {
        return buffer.readShortLe()
    }

    fun readUShortLe(): UShort {
        return buffer.readShortLe().toUShort()
    }

    fun readInt(): Int {
        return buffer.readInt()
    }

    fun readUInt(): UInt {
        return buffer.readInt().toUInt()
    }

    fun readIntLe(): Int {
        return buffer.readIntLe()
    }

    fun readUIntLe(): UInt {
        return buffer.readIntLe().toUInt()
    }

    fun readLong(): Long {
        return buffer.readLong()
    }

    fun readULong(): ULong {
        return buffer.readLong().toULong()
    }

    fun readLongLe(): Long {
        return buffer.readLongLe()
    }

    fun readULongLe(): ULong {
        return buffer.readLongLe().toULong()
    }

    fun readFloat(): Float {
        return Float.fromBits(readInt())
    }

    fun readDouble(): Double {
        return Double.fromBits(readLong())
    }

    fun readFloatLe(): Float {
        return Float.fromBits(readIntLe())
    }

    fun readDoubleLe(): Double {
        return Double.fromBits(readLongLe())
    }

    inline fun <reified E : Enum<E>> readEnum(): E {
        return enumValues<E>()[buffer.readInt()]
    }

    inline fun <reified E : Enum<E>> readEnumLe(): E {
        return enumValues<E>()[buffer.readIntLe()]
    }

    fun readBooleanArray(byteCount: Int): BooleanArray {
        val sink = BooleanArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readByte().toInt() != 0
        return sink
    }

    fun readBooleanArray(sink: BooleanArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readByte().toInt() != 0
    }

    fun readByteArray(byteCount: Int): ByteArray {
        return buffer.readByteArray(byteCount.toLong())
    }

    fun readByteArray(sink: ByteArray, offset: Int = 0, byteCount: Int = sink.size) {
        buffer.read(sink, offset, byteCount)
    }

    fun readUByteArray(byteCount: Int): UByteArray {
        val sink = UByteArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readUByte()
        return sink
    }

    fun readUByteArray(sink: UByteArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readUByte()
    }

    fun readShortArray(byteCount: Int): ShortArray {
        val sink = ShortArray(byteCount)
        for (i in 0 until byteCount) sink[i] = buffer.readShort()
        return sink
    }

    fun readShortArray(sink: ShortArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = buffer.readShort()
    }

    fun readUShortArray(byteCount: Int): UShortArray {
        val sink = UShortArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readUShort()
        return sink
    }

    fun readUShortArray(sink: UShortArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readUShort()
    }

    fun readIntArray(byteCount: Int): IntArray {
        val sink = IntArray(byteCount)
        for (i in 0 until byteCount) sink[i] = buffer.readInt()
        return sink
    }

    fun readIntArray(sink: IntArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = buffer.readInt()
    }

    fun readUIntArray(byteCount: Int): UIntArray {
        val sink = UIntArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readUInt()
        return sink
    }

    fun readUIntArray(sink: UIntArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readUInt()
    }

    fun readLongArray(byteCount: Int): LongArray {
        val sink = LongArray(byteCount)
        for (i in 0 until byteCount) sink[i] = buffer.readLong()
        return sink
    }

    fun readLongArray(sink: LongArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = buffer.readLong()
    }

    fun readULongArray(byteCount: Int): ULongArray {
        val sink = ULongArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readULong()
        return sink
    }

    fun readULongArray(sink: ULongArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readULong()
    }

    fun readFloatArray(byteCount: Int): FloatArray {
        val sink = FloatArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readFloat()
        return sink
    }

    fun readFloatArray(sink: FloatArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readFloat()
    }

    fun readDoubleArray(byteCount: Int): DoubleArray {
        val sink = DoubleArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readDouble()
        return sink
    }

    fun readDoubleArray(sink: DoubleArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readDouble()
    }

    fun readAscii(): String {
        return readString(Charsets.US_ASCII)
    }

    fun readUtf8(): String {
        return readString(Charsets.UTF_8)
    }

    fun readString(charset: Charset): String {
        val length = readInt()
        return buffer.readString(length.toLong(), charset)
    }

    fun readByteStream(): ByteStream {
        val size = readInt()
        val data = buffer.readByteArray(size.toLong())
        val buffer = Buffer()
        buffer.write(data)
        return ByteStream(buffer)
    }

    fun readShortArrayLe(byteCount: Int): ShortArray {
        val sink = ShortArray(byteCount)
        for (i in 0 until byteCount) sink[i] = buffer.readShortLe()
        return sink
    }

    fun readShortArrayLe(sink: ShortArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = buffer.readShortLe()
    }

    fun readUShortArrayLe(byteCount: Int): UShortArray {
        val sink = UShortArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readUShortLe()
        return sink
    }

    fun readUShortArrayLe(sink: UShortArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readUShortLe()
    }

    fun readIntArrayLe(byteCount: Int): IntArray {
        val sink = IntArray(byteCount)
        for (i in 0 until byteCount) sink[i] = buffer.readIntLe()
        return sink
    }

    fun readIntArrayLe(sink: IntArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = buffer.readIntLe()
    }

    fun readUIntArrayLe(byteCount: Int): UIntArray {
        val sink = UIntArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readUIntLe()
        return sink
    }

    fun readUIntArrayLe(sink: UIntArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readUIntLe()
    }

    fun readLongArrayLe(byteCount: Int): LongArray {
        val sink = LongArray(byteCount)
        for (i in 0 until byteCount) sink[i] = buffer.readLongLe()
        return sink
    }

    fun readLongArrayLe(sink: LongArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = buffer.readLongLe()
    }

    fun readULongArrayLe(byteCount: Int): ULongArray {
        val sink = ULongArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readULongLe()
        return sink
    }

    fun readULongArrayLe(sink: ULongArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readULongLe()
    }

    fun readFloatArrayLe(byteCount: Int): FloatArray {
        val sink = FloatArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readFloatLe()
        return sink
    }

    fun readFloatArrayLe(sink: FloatArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readFloatLe()
    }

    fun readDoubleArrayLe(byteCount: Int): DoubleArray {
        val sink = DoubleArray(byteCount)
        for (i in 0 until byteCount) sink[i] = readDoubleLe()
        return sink
    }

    fun readDoubleArrayLe(sink: DoubleArray, offset: Int = 0, byteCount: Int = sink.size) {
        for (i in offset until offset + byteCount) sink[i] = readDoubleLe()
    }
}