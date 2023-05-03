import br.tiagohm.nestalgia.core.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BitwiseTest {
    @Test
    fun makeUshort() {
        assertEquals(0xFFFFU.toUShort(), makeUShort(0xFFU, 0xFFU))
    }

    @Test
    fun bit() {
        var byte: UByte = 0x55U
        assertEquals(true, byte.bit0)
        assertEquals(false, byte.bit1)
        assertEquals(true, byte.bit2)
        assertEquals(false, byte.bit3)
        assertEquals(true, byte.bit4)
        assertEquals(false, byte.bit5)
        assertEquals(true, byte.bit6)
        assertEquals(false, byte.bit7)

        byte = 0xAAU
        assertEquals(false, byte.bit0)
        assertEquals(true, byte.bit1)
        assertEquals(false, byte.bit2)
        assertEquals(true, byte.bit3)
        assertEquals(false, byte.bit4)
        assertEquals(true, byte.bit5)
        assertEquals(false, byte.bit6)
        assertEquals(true, byte.bit7)
    }

    @Test
    fun lo() {
        val a: UShort = 0xFFAAU
        assertEquals(0xAAU.toUByte(), a.loByte)
    }

    @Test
    fun hi() {
        val a: UShort = 0x55AAU
        assertEquals(0x55U.toUByte(), a.hiByte)
    }

    @Test
    fun shr() {
        val a: UByte = 0xFFU
        assertEquals(0x7F.toUByte(), a shr 1)
    }
}