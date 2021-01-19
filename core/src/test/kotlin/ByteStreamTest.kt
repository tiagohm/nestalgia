import br.tiagohm.nestalgia.core.ByteStream
import br.tiagohm.nestalgia.core.ConsoleType
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
class ByteStreamTest {

    @Test
    fun writeAndRead() {
        val s = ByteStream()

        assertEquals(true, s.isEof)
        assertEquals(true, s.isEmpty)

        s.write(false)
        s.write(Byte.MAX_VALUE)
        s.write(UByte.MAX_VALUE)
        s.write(Short.MAX_VALUE)
        s.write(UShort.MAX_VALUE)
        s.write(Int.MAX_VALUE)
        s.write(UInt.MAX_VALUE)
        s.write(Long.MAX_VALUE)
        s.write(ULong.MAX_VALUE)
        s.write(Float.MAX_VALUE)
        s.write(Double.MAX_VALUE)
        s.writeAscii("Tiago")
        s.writeUtf8("Giovanna")
        s.write(ConsoleType.FAMICOM)
        s.write(booleanArrayOf(true, false))
        s.write(byteArrayOf(Byte.MIN_VALUE, Byte.MAX_VALUE))
        s.write(ubyteArrayOf(UByte.MIN_VALUE, UByte.MAX_VALUE))
        s.write(shortArrayOf(Short.MIN_VALUE, Short.MAX_VALUE))
        s.write(ushortArrayOf(UShort.MIN_VALUE, UShort.MAX_VALUE))
        s.write(intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE))
        s.write(uintArrayOf(UInt.MIN_VALUE, UInt.MAX_VALUE))
        s.write(longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE))
        s.write(ulongArrayOf(ULong.MIN_VALUE, ULong.MAX_VALUE))

        s.writeLe(Short.MAX_VALUE)
        s.writeLe(UShort.MAX_VALUE)
        s.writeLe(Int.MAX_VALUE)
        s.writeLe(UInt.MAX_VALUE)
        s.writeLe(Long.MAX_VALUE)
        s.writeLe(ULong.MAX_VALUE)
        s.writeLe(Float.MAX_VALUE)
        s.writeLe(Double.MAX_VALUE)
        s.writeLe(ConsoleType.FAMICOM)
        s.writeLe(shortArrayOf(Short.MIN_VALUE, Short.MAX_VALUE))
        s.writeLe(ushortArrayOf(UShort.MIN_VALUE, UShort.MAX_VALUE))
        s.writeLe(intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE))
        s.writeLe(uintArrayOf(UInt.MIN_VALUE, UInt.MAX_VALUE))
        s.writeLe(longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE))
        s.writeLe(ulongArrayOf(ULong.MIN_VALUE, ULong.MAX_VALUE))

        assertEquals(false, s.isEof)
        assertEquals(false, s.isEmpty)

        assertEquals(false, s.readBoolean())
        assertEquals(Byte.MAX_VALUE, s.readByte())
        assertEquals(UByte.MAX_VALUE, s.readUByte())
        assertEquals(Short.MAX_VALUE, s.readShort())
        assertEquals(UShort.MAX_VALUE, s.readUShort())
        assertEquals(Int.MAX_VALUE, s.readInt())
        assertEquals(UInt.MAX_VALUE, s.readUInt())
        assertEquals(Long.MAX_VALUE, s.readLong())
        assertEquals(ULong.MAX_VALUE, s.readULong())
        assertEquals(Float.MAX_VALUE, s.readFloat())
        assertEquals(Double.MAX_VALUE, s.readDouble())
        assertEquals("Tiago", s.readAscii())
        assertEquals("Giovanna", s.readUtf8())
        assertEquals(ConsoleType.FAMICOM, s.readEnum<ConsoleType>())
        assertArrayEquals(booleanArrayOf(true, false), s.readBooleanArray(2))
        assertArrayEquals(byteArrayOf(Byte.MIN_VALUE, Byte.MAX_VALUE), s.readByteArray(2))
        assertArrayEquals(
            byteArrayOf(UByte.MIN_VALUE.toByte(), UByte.MAX_VALUE.toByte()),
            s.readUByteArray(2).toByteArray()
        )
        assertArrayEquals(shortArrayOf(Short.MIN_VALUE, Short.MAX_VALUE), s.readShortArray(2))
        assertArrayEquals(
            shortArrayOf(UShort.MIN_VALUE.toShort(), UShort.MAX_VALUE.toShort()),
            s.readUShortArray(2).toShortArray()
        )
        assertArrayEquals(intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE), s.readIntArray(2))
        assertArrayEquals(intArrayOf(UInt.MIN_VALUE.toInt(), UInt.MAX_VALUE.toInt()), s.readUIntArray(2).toIntArray())
        assertArrayEquals(longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE), s.readLongArray(2))
        assertArrayEquals(
            longArrayOf(ULong.MIN_VALUE.toLong(), ULong.MAX_VALUE.toLong()),
            s.readULongArray(2).toLongArray()
        )

        assertEquals(Short.MAX_VALUE, s.readShortLe())
        assertEquals(UShort.MAX_VALUE, s.readUShortLe())
        assertEquals(Int.MAX_VALUE, s.readIntLe())
        assertEquals(UInt.MAX_VALUE, s.readUIntLe())
        assertEquals(Long.MAX_VALUE, s.readLongLe())
        assertEquals(ULong.MAX_VALUE, s.readULongLe())
        assertEquals(Float.MAX_VALUE, s.readFloatLe())
        assertEquals(Double.MAX_VALUE, s.readDoubleLe())
        assertEquals(ConsoleType.FAMICOM, s.readEnumLe<ConsoleType>())
        assertArrayEquals(shortArrayOf(Short.MIN_VALUE, Short.MAX_VALUE), s.readShortArrayLe(2))
        assertArrayEquals(
            shortArrayOf(UShort.MIN_VALUE.toShort(), UShort.MAX_VALUE.toShort()),
            s.readUShortArrayLe(2).toShortArray()
        )
        assertArrayEquals(intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE), s.readIntArrayLe(2))
        assertArrayEquals(intArrayOf(UInt.MIN_VALUE.toInt(), UInt.MAX_VALUE.toInt()), s.readUIntArrayLe(2).toIntArray())
        assertArrayEquals(longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE), s.readLongArrayLe(2))
        assertArrayEquals(
            longArrayOf(ULong.MIN_VALUE.toLong(), ULong.MAX_VALUE.toLong()),
            s.readULongArrayLe(2).toLongArray()
        )

        assertEquals(true, s.isEof)
        assertEquals(true, s.isEmpty)

        System.err.println(s.bytes.contentToString())

    }
}