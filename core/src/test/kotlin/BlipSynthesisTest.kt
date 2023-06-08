import br.tiagohm.nestalgia.core.Blip
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe

class BlipSynthesisTest : StringSpec() {

    init {
        "add delta fast and read samples" {
            val blip = Blip(BLIP_SIZE)

            blip.addDeltaFast(2 * OVERSAMPLE, 16384)
            blip.endFrameAndReadSamples() shouldBe shortArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16384, 16352, 16320,
                16288, 16256, 16224, 16192, 16161, 16129, 16098,
                16066, 16035, 16004, 15972, 15941,
                15910, 15879, 15848, 15817, 15786, 15755, 15724
            )

            blip.addDeltaFast((2.5 * OVERSAMPLE).toInt(), 16384)
            blip.endFrameAndReadSamples() shouldBe shortArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8192, 16368,
                16336, 16304, 16272, 16240, 16208, 16177,
                16145, 16113, 16082, 16051, 16019, 15988, 15957,
                15926, 15894, 15863, 15832, 15802, 15771, 15740
            )
        }
        "add delta tails" {
            val blip = Blip(BLIP_SIZE)

            blip.addDelta(0, 16384)
            blip.endFrameAndReadSamples() shouldBe shortArrayOf(
                0, 21, -37, 139, -106, 462, 5, 2935, 13440, 16345,
                15856, 16393, 16117, 16260, 16171, 16161, 16129,
                16098, 16066, 16035, 16004, 15972, 15941, 15910, 15879,
                15848, 15817, 15786, 15755, 15725, 15694, 15663
            )

            blip.addDelta(OVERSAMPLE / 2, 16384)
            blip.endFrameAndReadSamples() shouldBe shortArrayOf(
                0, 16, -15, 42, 59, 29, 532, 190, 8190, 16174,
                15801, 16273, 16212, 16197, 16223, 16161,
                16145, 16114, 16082, 16051, 16019, 15988,
                15957, 15926, 15895, 15863, 15832, 15802,
                15771, 15740, 15709, 15678
            )
        }
        "add delta interpolation" {
            val blip = Blip(BLIP_SIZE)

            blip.addDelta(OVERSAMPLE / 2, 32768)
            blip.endFrameAndReadSamples() shouldBe shortArrayOf(
                0, 32, -30, 85, 119, 59, 1065, 380, 16380, 32349,
                31603, 32547, 32424, 32394, 32446, 32322,
                32291, 32228, 32165, 32102, 32039, 31976,
                31914, 31852, 31789, 31727, 31665, 31604, 31542,
                31480, 31419, 31357
            )

            blip.addDelta(OVERSAMPLE / 2 + OVERSAMPLE / 64, 32768)
            blip.endFrameAndReadSamples() shouldBe shortArrayOf(
                0, 30, -27, 78, 130, 38, 1083, 308, 16019, 32267,
                31624, 32525, 32436, 32388, 32450, 32322, 32292,
                32229, 32166, 32103, 32040, 31977, 31915, 31853,
                31790, 31728, 31666, 31605, 31543, 31481, 31420, 31358
            )

            blip.addDelta(OVERSAMPLE / 2 + OVERSAMPLE / 32, 32768)
            blip.endFrameAndReadSamples() shouldBe shortArrayOf(
                0, 29, -24, 70, 140, 17, 1100, 236, 15657, 32185,
                31646, 32503, 32448, 32381, 32453, 32322, 32293,
                32230, 32167, 32104, 32041, 31978, 31916, 31854,
                31791, 31729, 31667, 31606, 31544, 31482, 31421, 31359
            )
        }
        "saturation" {
            val buffer = ShortArray(BLIP_SIZE)
            val blip = Blip(BLIP_SIZE)

            blip.addDeltaFast(0, 35000)
            blip.endFrame(OVERSAMPLE * BLIP_SIZE)
            blip.readSample(buffer, BLIP_SIZE, false)
            buffer[20].toInt() shouldBeExactly 32767

            blip.clear()

            blip.addDeltaFast(0, -35000)
            blip.endFrame(OVERSAMPLE * BLIP_SIZE)
            blip.readSample(buffer, BLIP_SIZE, false)
            buffer[20].toInt() shouldBeExactly -32768
        }
        "stereo interleave" {
            val buffer = ShortArray(BLIP_SIZE)
            val stereoBuffer = ShortArray(BLIP_SIZE * 2)
            val blip = Blip(BLIP_SIZE)

            blip.addDelta(0, 16384)
            blip.endFrame(BLIP_SIZE * OVERSAMPLE)
            blip.readSample(buffer, BLIP_SIZE, false)

            blip.clear()

            blip.addDelta(0, 16384)
            blip.endFrame(BLIP_SIZE * OVERSAMPLE)
            blip.readSample(stereoBuffer, BLIP_SIZE, true)

            for (i in 0 until BLIP_SIZE) {
                stereoBuffer[i * 2].toInt() shouldBeExactly buffer[i].toInt()
            }
        }
        "clear" {
            val buffer = ShortArray(BLIP_SIZE)
            val blip = Blip(BLIP_SIZE)

            blip.addDelta(0, 32768)
            blip.addDelta((BLIP_SIZE + 2) * OVERSAMPLE + OVERSAMPLE / 2, 32768)

            blip.clear()

            var n = 2

            while (n-- > 0) {
                blip.endFrame(BLIP_SIZE * OVERSAMPLE)
                blip.readSample(buffer, BLIP_SIZE, false) shouldBeExactly BLIP_SIZE
                buffer.forEach { it.toInt() shouldBeExactly 0 }
            }
        }
    }

    companion object {

        private const val OVERSAMPLE = Blip.BLIP_MAX_RATIO
        private const val BLIP_SIZE = 32

        @JvmStatic
        private fun Blip.endFrameAndReadSamples(): ShortArray {
            val buffer = ShortArray(BLIP_SIZE)
            endFrame(BLIP_SIZE * OVERSAMPLE)
            readSample(buffer, BLIP_SIZE, false)
            clear()
            return buffer
        }
    }
}
