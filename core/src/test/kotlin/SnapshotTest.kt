import br.tiagohm.nestalgia.core.ConsoleType
import br.tiagohm.nestalgia.core.Region
import br.tiagohm.nestalgia.core.Snapshot
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SnapshotTest {

    fun makeSnapshot(): Snapshot {
        val s = Snapshot()

        s.write("a", false)
        s.write("b", Byte.MAX_VALUE)
        s.write("c", UByte.MAX_VALUE)
        s.write("d", Short.MAX_VALUE)
        s.write("e", UShort.MAX_VALUE)
        s.write("f", Int.MAX_VALUE)
        s.write("g", UInt.MAX_VALUE)
        s.write("h", Long.MAX_VALUE)
        s.write("i", ULong.MAX_VALUE)
        s.write("j", Float.MAX_VALUE)
        s.write("k", Double.MAX_VALUE)

        s.writeAscii("l", "Tiago")
        s.writeUtf8("m", "Giovanna")
        s.write("n", ConsoleType.FAMICOM)

        s.write("o", booleanArrayOf(true, false))
        s.write("p", byteArrayOf(Byte.MIN_VALUE, Byte.MAX_VALUE))
        s.write("q", ubyteArrayOf(UByte.MIN_VALUE, UByte.MAX_VALUE))
        s.write("r", shortArrayOf(Short.MIN_VALUE, Short.MAX_VALUE))
        s.write("s", ushortArrayOf(UShort.MIN_VALUE, UShort.MAX_VALUE))
        s.write("t", intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE))
        s.write("u", uintArrayOf(UInt.MIN_VALUE, UInt.MAX_VALUE))
        s.write("v", longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE))
        s.write("w", ulongArrayOf(ULong.MIN_VALUE, ULong.MAX_VALUE))
        s.write("x", floatArrayOf(Float.MIN_VALUE, Float.MAX_VALUE))
        s.write("y", doubleArrayOf(Double.MIN_VALUE, Double.MAX_VALUE))
        s.write("z", arrayOf(Region.NTSC, Region.DENDY))

        return s
    }

    @Test
    fun writeAndRead() {
        val s0 = makeSnapshot()
        val s1 = makeSnapshot()

        s0.write("aa", s1)

        assertEquals(27, s0.load())
        assertEquals(26, s1.load())

        val s2 = s0.readSnapshot("aa")!!
        assertEquals(26, s2.load())

        testRead(s0)
        testRead(s1)
        testRead(s2)
    }

    fun testRead(s: Snapshot) {
        assertEquals(false, s.readBoolean("a"))
        assertEquals(Byte.MAX_VALUE, s.readByte("b"))
        assertEquals(UByte.MAX_VALUE, s.readUByte("c"))
        assertEquals(Short.MAX_VALUE, s.readShort("d"))
        assertEquals(UShort.MAX_VALUE, s.readUShort("e"))
        assertEquals(Int.MAX_VALUE, s.readInt("f"))
        assertEquals(UInt.MAX_VALUE, s.readUInt("g"))
        assertEquals(Long.MAX_VALUE, s.readLong("h"))
        assertEquals(ULong.MAX_VALUE, s.readULong("i"))
        assertEquals(Float.MAX_VALUE, s.readFloat("j"))
        assertEquals(Double.MAX_VALUE, s.readDouble("k"))

        assertEquals("Tiago", s.readAscii("l"))
        assertEquals("Giovanna", s.readUtf8("m"))
        assertEquals(ConsoleType.FAMICOM, s.readEnum<ConsoleType>("n"))

        assertArrayEquals(booleanArrayOf(true, false), s.readBooleanArray("o"))
        assertArrayEquals(byteArrayOf(Byte.MIN_VALUE, Byte.MAX_VALUE), s.readByteArray("p"))
        assertArrayEquals(
            byteArrayOf(UByte.MIN_VALUE.toByte(), UByte.MAX_VALUE.toByte()),
            s.readUByteArray("q")?.toByteArray()
        )
        assertArrayEquals(shortArrayOf(Short.MIN_VALUE, Short.MAX_VALUE), s.readShortArray("r"))
        assertArrayEquals(
            shortArrayOf(UShort.MIN_VALUE.toShort(), UShort.MAX_VALUE.toShort()),
            s.readUShortArray("s")?.toShortArray()
        )
        assertArrayEquals(intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE), s.readIntArray("t"))
        assertArrayEquals(
            intArrayOf(UInt.MIN_VALUE.toInt(), UInt.MAX_VALUE.toInt()),
            s.readUIntArray("u")?.toIntArray()
        )
        assertArrayEquals(longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE), s.readLongArray("v"))
        assertArrayEquals(
            longArrayOf(ULong.MIN_VALUE.toLong(), ULong.MAX_VALUE.toLong()),
            s.readULongArray("w")?.toLongArray()
        )
        assertArrayEquals(floatArrayOf(Float.MIN_VALUE, Float.MAX_VALUE), s.readFloatArray("x"))
        assertArrayEquals(doubleArrayOf(Double.MIN_VALUE, Double.MAX_VALUE), s.readDoubleArray("y"))

        assertArrayEquals(arrayOf(Region.NTSC, Region.DENDY), s.readEnumArray<Region>("z"))
    }
}