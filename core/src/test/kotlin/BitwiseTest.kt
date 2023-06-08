import br.tiagohm.nestalgia.core.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly

class BitwiseTest : StringSpec() {

    init {
        "bit" {
            0x55.bit0.shouldBeTrue()
            0x55.bit1.shouldBeFalse()
            0x55.bit2.shouldBeTrue()
            0x55.bit3.shouldBeFalse()
            0x55.bit4.shouldBeTrue()
            0x55.bit5.shouldBeFalse()
            0x55.bit6.shouldBeTrue()
            0x55.bit7.shouldBeFalse()

            0xAA.bit0.shouldBeFalse()
            0xAA.bit1.shouldBeTrue()
            0xAA.bit2.shouldBeFalse()
            0xAA.bit3.shouldBeTrue()
            0xAA.bit4.shouldBeFalse()
            0xAA.bit5.shouldBeTrue()
            0xAA.bit6.shouldBeFalse()
            0xAA.bit7.shouldBeTrue()
        }
        "loByte" {
            0x55AA33BB.loByte shouldBeExactly 0xBB
        }
        "hiByte" {
            0x55AA33BB.hiByte shouldBeExactly 0x33
        }
        "higherByte" {
            0x55AA33BB.higherByte shouldBeExactly 0xAA
        }
        "highestByte" {
            0x55AA33BB.highestByte shouldBeExactly 0x55
        }
    }
}
