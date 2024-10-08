import br.tiagohm.nestalgia.core.Blip
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe

class BlipInvarianceTest : StringSpec() {

    init {
        "end frame and add delta" {
            val zero = ShortArray(BLIP_SIZE) { 1 }
            val one = ShortArray(BLIP_SIZE) { -1 }

            val blip0 = Blip(BLIP_SIZE)
            blip0.addDeltas(0)
            blip0.addDeltas(FRAME_LENGTH)
            blip0.endFrame(FRAME_LENGTH * 2)
            blip0.readSample(zero, BLIP_SIZE, false) shouldBeExactly BLIP_SIZE

            val blip1 = Blip(BLIP_SIZE)
            blip1.addDeltas(0)
            blip1.endFrame(FRAME_LENGTH)
            blip1.addDeltas(0)
            blip1.endFrame(FRAME_LENGTH)
            blip1.readSample(one, BLIP_SIZE, false) shouldBeExactly BLIP_SIZE

            zero shouldBe one
        }
        "read samples" {
            val blipSize = (FRAME_LENGTH * 3) / OVERSAMPLE
            val zero = ShortArray(blipSize) { 1 }
            val one = ShortArray(blipSize) { -1 }

            val blip0 = Blip(blipSize)
            blip0.addDeltas(0 * FRAME_LENGTH)
            blip0.addDeltas(1 * FRAME_LENGTH)
            blip0.addDeltas(2 * FRAME_LENGTH)
            blip0.endFrame(3 * FRAME_LENGTH)
            blip0.readSample(zero, blipSize, false) shouldBeExactly blipSize

            zero shouldBe shortArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -3, 7, -5, 21,
                9, 119, 750, 1004, 963, 1001, 983, 993,
                985, 992, 982, 997, 999, 1050, 1649, 1982,
                1932, 1976, 1955, 1966, 1980, 1986, 2534,
                2944, 2900, 2933, 2918, 2919, 2912, 2909,
                2905, 2898, 2923, 2896, 3379, 3861, 3832,
                3856, 3852, 3837, 3871, 3824, 4232, 4775,
            )

            val blip1 = Blip(blipSize / 3)
            var count = 0

            repeat(3) {
                blip1.addDeltas(0)
                blip1.endFrame(FRAME_LENGTH)
                count += blip1.readSample(one, blipSize - count, false, count)
            }

            count shouldBeExactly blipSize
            zero shouldBe one
        }
        "max frame" {
            val oversample = 32
            val frameLength = Blip.BLIP_MAX_FRAME * oversample
            val blipSize = frameLength / oversample * 3

            val one = ShortArray(blipSize) { 1 }
            val two = ShortArray(blipSize) { -1 }

            run {
                val blip = Blip(blipSize)
                blip.rates(oversample.toDouble(), 1.0)

                var count = 0

                repeat(3) {
                    blip.endFrame(frameLength / 2)
                    blip.addDelta(frameLength / 2 + Blip.END_FRAME_EXTRA * oversample, 1000)
                    blip.endFrame(frameLength / 2)

                    count += blip.readSample(one, blipSize - count, false, count)
                }

                count shouldBeExactly blipSize
            }

            run {
                val blip = Blip(blipSize)
                blip.rates(oversample.toDouble(), 1.0)

                var count = 0

                repeat(3) {
                    blip.addDelta(frameLength + Blip.END_FRAME_EXTRA * oversample, 1000)
                    blip.endFrame(frameLength)

                    count += blip.readSample(two, blipSize - count, false, count)
                }

                count shouldBeExactly blipSize
            }

            one shouldBe two
        }
    }

    companion object {

        private const val OVERSAMPLE = Blip.BLIP_MAX_RATIO
        private const val FRAME_LENGTH = 20 * OVERSAMPLE + OVERSAMPLE / 4
        private const val BLIP_SIZE = (FRAME_LENGTH * 2) / OVERSAMPLE

        private fun Blip.addDeltas(offset: Int) {
            addDelta(FRAME_LENGTH / 2 + offset, 1000)
            addDelta(FRAME_LENGTH + offset + Blip.END_FRAME_EXTRA * OVERSAMPLE, 1000)
        }
    }
}
