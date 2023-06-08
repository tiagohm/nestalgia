import br.tiagohm.nestalgia.core.Blip
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import kotlin.math.abs

// https://github.com/nesbox/blip-buf/tree/master/tests

class BlipTest : StringSpec() {

    init {
        "end frame and samples avail" {
            val blip = Blip(BLIP_SIZE)
            blip.endFrame(OVERSAMPLE)
            blip.avail shouldBeExactly 1

            blip.endFrame(OVERSAMPLE * 2)
            blip.avail shouldBeExactly 3
        }
        "end frame and samples avail fractional" {
            val blip = Blip(BLIP_SIZE)
            blip.endFrame(OVERSAMPLE * 2 - 1)
            blip.avail shouldBeExactly 1

            blip.endFrame(1)
            blip.avail shouldBeExactly 2
        }
        "end frame limits" {
            val blip = Blip(BLIP_SIZE)
            blip.endFrame(0)
            blip.avail shouldBeExactly 0

            blip.endFrame(BLIP_SIZE * OVERSAMPLE + OVERSAMPLE - 1)
            shouldThrow<IllegalStateException> { blip.endFrame(1) }
        }
        "clock needs" {
            val blip = Blip(BLIP_SIZE)
            blip.clocksNeeded(0) shouldBeExactly 0L * OVERSAMPLE
            blip.clocksNeeded(2) shouldBeExactly 2L * OVERSAMPLE

            blip.endFrame(1)

            blip.clocksNeeded(0) shouldBeExactly 0L
            blip.clocksNeeded(2) shouldBeExactly 2L * OVERSAMPLE - 1
        }
        "clock needs limits" {
            val blip = Blip(BLIP_SIZE)

            shouldThrow<IllegalStateException> { blip.clocksNeeded(-1) }

            blip.endFrame(OVERSAMPLE * 2 - 1)
            blip.clocksNeeded(BLIP_SIZE - 1) shouldBeExactly (BLIP_SIZE - 2) * OVERSAMPLE + 1L

            blip.endFrame(1)

            shouldThrow<IllegalStateException> { blip.clocksNeeded(BLIP_SIZE - 1) }
        }
        "clear" {
            val blip = Blip(BLIP_SIZE)

            blip.endFrame(OVERSAMPLE * 2 - 1)
            blip.clear()

            blip.avail shouldBeExactly 0
            blip.clocksNeeded(1) shouldBeExactly OVERSAMPLE.toLong()
        }
        "read samples" {
            val blip = Blip(BLIP_SIZE)

            val buf = ShortArray(2) { -1 }

            blip.endFrame(3 * OVERSAMPLE + OVERSAMPLE - 1)
            blip.readSample(buf, 2, false) shouldBeExactly 2
            buf[0].toInt() shouldBeExactly 0
            buf[1].toInt() shouldBeExactly 0
            blip.avail shouldBeExactly 1
            blip.clocksNeeded(1) shouldBeExactly 1
        }
        "read samples stereo" {
            val blip = Blip(BLIP_SIZE)

            val buf = ShortArray(3) { -1 }

            blip.endFrame(2 * OVERSAMPLE)
            blip.readSample(buf, 2, true) shouldBeExactly 2
            buf[0].toInt() shouldBeExactly 0
            buf[1].toInt() shouldBeExactly -1
            buf[2].toInt() shouldBeExactly 0
        }
        "read samples limits to avail" {
            val blip = Blip(BLIP_SIZE)

            blip.endFrame(2 * OVERSAMPLE)

            val buf = ShortArray(2) { -1 }

            blip.readSample(buf, 3, false) shouldBeExactly 2
            blip.avail shouldBeExactly 0
            buf[0].toInt() shouldBeExactly 0
            buf[1].toInt() shouldBeExactly 0
        }
        "read samples limits" {
            val blip = Blip(BLIP_SIZE)

            val buf = ShortArray(0)

            blip.readSample(buf, 1, false) shouldBeExactly 0

            shouldThrow<IllegalArgumentException> { blip.readSample(buf, -1, false) }
        }
        "rates" {
            val blip = Blip(BLIP_SIZE)

            blip.rates(2.0, 2.0)
            blip.clocksNeeded(10) shouldBeExactly 10

            blip.rates(2.0, 4.0)
            blip.clocksNeeded(10) shouldBeExactly 5

            blip.rates(4.0, 2.0)
            blip.clocksNeeded(10) shouldBeExactly 20
        }
        "rates rounds sample rate up" {
            val blip = Blip(BLIP_SIZE)

            repeat(10000) {
                blip.rates(it.toDouble(), 1.0)
                blip.clocksNeeded(1) shouldBeLessThanOrEqual it.toLong()
            }
        }
        "rates accuracy" {
            val blip = Blip(BLIP_SIZE)

            for (r in BLIP_SIZE / 2 until BLIP_SIZE) {
                var c = r / 2

                while (c < 8000000) {
                    blip.rates(c.toDouble(), r.toDouble())
                    val error = blip.clocksNeeded(r) - c
                    abs(error) shouldBeLessThan (c / MAX_ERROR).toLong()
                    c += c / 32
                }
            }
        }
        "rates high accuracy" {
            val blip = Blip(BLIP_SIZE)

            blip.rates(1000000.0, BLIP_SIZE.toDouble())
            blip.clocksNeeded(BLIP_SIZE) shouldBeExactly 1000000

            for (r in BLIP_SIZE / 2 until BLIP_SIZE) {
                var c = r / 2L

                while (c < 200000000) {
                    blip.rates(c.toDouble(), r.toDouble())
                    blip.clocksNeeded(r) shouldBeExactly c
                    c += c / 32
                }
            }
        }
        "rates long term accuracy" {
            val blip = Blip(BLIP_SIZE)

            blip.rates(1000000.0, BLIP_SIZE.toDouble())
            blip.clocksNeeded(BLIP_SIZE) shouldBeExactly 1000000

            // Generates secs seconds and ensures that exactly secs*sample_rate samples
            // are generated.
            val clockRate = 1789773.0
            val sampleRate = 44100
            val s = 1000

            blip.rates(clockRate, sampleRate.toDouble())

            // speeds test greatly when using protected malloc
            val bufSize = BLIP_SIZE / 2

            val clockSize = blip.clocksNeeded(bufSize) - 1

            var totalSamples = 0
            var remain = clockRate * s
            val buf = ShortArray(bufSize)

            while (true) {
                val n = if (remain < clockSize) remain.toLong() else clockSize

                if (n == 0L) break

                blip.endFrame(n.toInt())
                totalSamples += blip.readSample(buf, bufSize, false)

                remain -= n.toDouble()
            }

            totalSamples shouldBeExactly sampleRate * s
        }
        "add delta limits" {
            val blip = Blip(BLIP_SIZE)

            blip.addDelta(0, 1)
            blip.addDelta((BLIP_SIZE + 3) * OVERSAMPLE - 1, 1)

            shouldThrow<IllegalStateException> { blip.addDelta((BLIP_SIZE + 3) * OVERSAMPLE, 1) }
            shouldThrow<IllegalStateException> { blip.addDelta(-1, 1) }
        }
        "add delta fast limits" {
            val blip = Blip(BLIP_SIZE)

            blip.addDeltaFast(0, 1)
            blip.addDeltaFast((BLIP_SIZE + 3) * OVERSAMPLE - 1, 1)

            shouldThrow<IllegalStateException> { blip.addDeltaFast((BLIP_SIZE + 3) * OVERSAMPLE, 1) }
            shouldThrow<IllegalStateException> { blip.addDeltaFast(-1, 1) }
        }
    }

    companion object {

        private const val OVERSAMPLE = Blip.BLIP_MAX_RATIO
        private const val BLIP_SIZE = Blip.BLIP_MAX_FRAME / 2
        private const val MAX_ERROR = 100
    }
}
