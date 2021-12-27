import br.tiagohm.nestalgia.core.Blip
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs

class BlipTest {

    @Test
    fun endFrameAndSamplesAvail() {
        val blip = Blip(BLIP_SIZE)
        blip.endFrame(OVERSAMPLE)
        assertEquals(blip.avail, 1)

        blip.endFrame(OVERSAMPLE * 2)
        assertEquals(blip.avail, 3)
    }

    @Test
    fun endFrameAndSamplesAvailFractional() {
        val blip = Blip(BLIP_SIZE)
        blip.endFrame(OVERSAMPLE * 2 - 1)
        assertEquals(blip.avail, 1)

        blip.endFrame(1)
        assertEquals(blip.avail, 2)
    }

    @Test
    fun endFrameLimits() {
        val blip = Blip(BLIP_SIZE)
        blip.endFrame(0)
        assertEquals(blip.avail, 0)

        blip.endFrame(BLIP_SIZE * OVERSAMPLE + OVERSAMPLE - 1)

        assertThrows(IllegalStateException::class.java) {
            blip.endFrame(1)
        }
    }

    @Test
    fun clockNeeds() {
        val blip = Blip(BLIP_SIZE)
        assertEquals(0L * OVERSAMPLE, blip.clocksNeeded(0))
        assertEquals(2L * OVERSAMPLE, blip.clocksNeeded(2))

        blip.endFrame(1)

        assertEquals(0L, blip.clocksNeeded(0))
        assertEquals(2L * OVERSAMPLE - 1, blip.clocksNeeded(2))
    }

    @Test
    fun clockNeedsLimits() {
        val blip = Blip(BLIP_SIZE)

        assertThrows(IllegalStateException::class.java) {
            blip.clocksNeeded(-1)
        }

        blip.endFrame(OVERSAMPLE * 2 - 1)
        assertEquals(blip.clocksNeeded(BLIP_SIZE - 1), (BLIP_SIZE - 2) * OVERSAMPLE + 1L)

        blip.endFrame(1)

        assertThrows(IllegalStateException::class.java) {
            blip.clocksNeeded(BLIP_SIZE - 1)
        }
    }

    @Test
    fun clear() {
        val blip = Blip(BLIP_SIZE)

        blip.endFrame(OVERSAMPLE * 2 - 1)
        blip.clear()

        assertEquals(0, blip.avail)
        assertEquals(blip.clocksNeeded(1), OVERSAMPLE.toLong())
    }

    @Test
    fun readSamples() {
        val blip = Blip(BLIP_SIZE)

        val buf = ShortArray(2) { -1 }

        blip.endFrame(3 * OVERSAMPLE + OVERSAMPLE - 1)
        assertEquals(blip.readSample(buf, 2, false), 2)
        assertEquals(buf[0].toInt(), 0)
        assertEquals(buf[1].toInt(), 0)
        assertEquals(1, blip.avail)
        assertEquals(1, blip.clocksNeeded(1))
    }

    @Test
    fun readSamplesStereo() {
        val blip = Blip(BLIP_SIZE)

        val buf = ShortArray(3) { -1 }

        blip.endFrame(2 * OVERSAMPLE)
        assertEquals(blip.readSample(buf, 2, true), 2)
        assertEquals(buf[0].toInt(), 0)
        assertEquals(buf[1].toInt(), -1)
        assertEquals(buf[2].toInt(), 0)
    }

    @Test
    fun readSamplesLimitsToAvail() {
        val blip = Blip(BLIP_SIZE)

        blip.endFrame(2 * OVERSAMPLE)

        val buf = ShortArray(2) { -1 }

        assertEquals(blip.readSample(buf, 3, false), 2)
        assertEquals(blip.avail, 0)
        assertEquals(buf[0].toInt(), 0)
        assertEquals(buf[1].toInt(), 0)
    }

    @Test
    fun readSamplesLimits() {
        val blip = Blip(BLIP_SIZE)

        val buf = ShortArray(0)

        assertEquals(blip.readSample(buf, 1, false), 0)

        assertThrows(IllegalArgumentException::class.java) {
            blip.readSample(buf, -1, false)
        }
    }

    @Test
    fun setRates() {
        val blip = Blip(BLIP_SIZE)

        blip.setRates(2.0, 2.0)
        assertEquals(10, blip.clocksNeeded(10))

        blip.setRates(2.0, 4.0)
        assertEquals(5, blip.clocksNeeded(10))

        blip.setRates(4.0, 2.0)
        assertEquals(20, blip.clocksNeeded(10))
    }

    @Test
    fun setRatesRoundsSampleRateUp() {
        val blip = Blip(BLIP_SIZE)

        for (i in 0 until 10000) {
            blip.setRates(i.toDouble(), 1.0)
            assertTrue(blip.clocksNeeded(1) <= i)
        }
    }

    @Test
    fun setRatesAccuracy() {
        val blip = Blip(BLIP_SIZE)

        for (r in BLIP_SIZE / 2 until BLIP_SIZE) {
            var c = r / 2

            while (c < 8000000) {
                blip.setRates(c.toDouble(), r.toDouble())
                val error = blip.clocksNeeded(r) - c
                assertTrue(abs(error) < c / MAX_ERROR)
                c += c / 32
            }
        }
    }

    @Test
    fun setRatesHighAccuracy() {
        val blip = Blip(BLIP_SIZE)

        blip.setRates(1000000.0, BLIP_SIZE.toDouble())
        assertEquals(1000000, blip.clocksNeeded(BLIP_SIZE))

        for (r in BLIP_SIZE / 2 until BLIP_SIZE) {
            var c = r / 2L

            while (c < 200000000) {
                blip.setRates(c.toDouble(), r.toDouble())
                assertEquals(blip.clocksNeeded(r), c)
                c += c / 32
            }
        }
    }

    @Test
    fun setRatesLongTermAccuracy() {
        val blip = Blip(BLIP_SIZE)

        blip.setRates(1000000.0, BLIP_SIZE.toDouble())
        assertEquals(1000000, blip.clocksNeeded(BLIP_SIZE))

        // Generates secs seconds and ensures that exactly secs*sample_rate samples
        // are generated.
        val clockRate = 1789773.0
        val sampleRate = 44100
        val s = 1000

        blip.setRates(clockRate, sampleRate.toDouble())

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

        assertEquals(totalSamples, sampleRate * s)
    }

    @Test
    fun addDeltaLimits() {
        val blip = Blip(BLIP_SIZE)

        blip.addDelta(0, 1)
        blip.addDelta((BLIP_SIZE + 3) * OVERSAMPLE - 1, 1)

        assertThrows(IllegalStateException::class.java) {
            blip.addDelta((BLIP_SIZE + 3) * OVERSAMPLE, 1)
        }

        assertThrows(IllegalStateException::class.java) {
            blip.addDelta(-1, 1)
        }
    }

    @Test
    fun addDeltaFastLimits() {
        val blip = Blip(BLIP_SIZE)

        blip.addDeltaFast(0, 1)
        blip.addDeltaFast((BLIP_SIZE + 3) * OVERSAMPLE - 1, 1)

        assertThrows(IllegalStateException::class.java) {
            blip.addDeltaFast((BLIP_SIZE + 3) * OVERSAMPLE, 1)
        }

        assertThrows(IllegalStateException::class.java) {
            blip.addDeltaFast(-1, 1)
        }
    }

    companion object {
        const val OVERSAMPLE = Blip.BLIP_MAX_RATIO
        const val BLIP_SIZE = Blip.BLIP_MAX_FRAME / 2
        const val MAX_ERROR = 100
    }
}