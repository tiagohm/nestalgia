package br.tiagohm.nestalgia.core

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class Blip(private val size: Int) {

    @Volatile private var factor = TIME_UNIT / BLIP_MAX_RATIO
    @Volatile private var offset = 0L
    @Volatile private var integrator = 0
    private val buffer = IntArray(size + BUF_EXTRA)

    @Volatile var avail = 0
        private set

    init {
        clear()
    }

    fun clear() {
        offset = factor / 2
        avail = 0
        integrator = 0
        buffer.fill(0)
    }

    fun rates(clockRate: Double, sampleRate: Double) {
        factor = ceil(TIME_UNIT * sampleRate / clockRate).toLong()
    }

    fun clocksNeeded(samples: Int): Long {
        if (samples < 0 || avail + samples > size) {
            throw IllegalStateException("Buffer can't hold that many more samples")
        }

        val needed = samples * TIME_UNIT

        return if (needed < offset) 0L
        else (needed - offset + factor - 1) / factor
    }

    fun endFrame(t: Int) {
        val off = t * factor + offset
        avail += (off shr TIME_BITS).toInt()
        offset = off and (TIME_UNIT - 1)

        if (avail > size) {
            throw IllegalStateException("Buffer size was exceeded")
        }
    }

    fun removeSamples(count: Int) {
        val remain = avail + BUF_EXTRA - count
        avail -= count

        // memmove( &buf[0], &buf[count], remain * sizeof buf[0] );
        buffer.copyInto(buffer, 0, count, count + remain)

        // memset( &buf[remain], 0, count * sizeof buf[0] );
        buffer.fill(0, remain, remain + count)
    }

    fun readSample(outputBuffer: ShortArray, count: Int, stereo: Boolean, startIndex: Int = 0): Int {
        if (count < 0) {
            throw IllegalArgumentException("count should be greater or equal to zero")
        }

        val c = min(avail, count)

        if (c > 0) {
            val step = if (stereo) 2 else 1
            var sum = integrator
            var out = 0

            for (i in 0 until c) {
                // Eliminate fraction
                val s = max(MIN_SAMPLE, min(sum shr DELTA_BITS, MAX_SAMPLE))

                sum += buffer[i]
                outputBuffer[startIndex + out] = s.toShort()

                out += step

                // High-pass filter
                sum -= s shl (DELTA_BITS - BASS_SHIFT)
            }

            integrator = sum

            removeSamples(c)
        }

        return c
    }

    fun addDelta(time: Int, delta: Int) {
        val off = time.toLong() * factor + offset
        val fixed = (off shr PRE_SHIFT) and 0xFFFFFFFF
        val out = avail + (fixed shr FRAC_BITS).toInt()
        val phase = (fixed shr PHASE_SHIFT).toInt() and (PHASE_COUNT - 1)
        val input0 = BL_STEP[phase]
        val input1 = BL_STEP[phase + 1]
        val rev0 = BL_STEP[PHASE_COUNT - phase]
        val rev1 = BL_STEP[PHASE_COUNT - phase - 1]
        val interp = fixed shr (PHASE_SHIFT - DELTA_BITS) and (DELTA_UNIT - 1)
        val delta2 = (delta * interp) shr DELTA_BITS
        val d = delta - delta2

        if (out > size + END_FRAME_EXTRA) {
            throw IllegalStateException("Buffer size was exceeded")
        }

        for (i in 0..7) {
            buffer[out + i] = (buffer[out + i] + input0[i] * d + input1[i] * delta2).toInt()
        }

        for (i in 0..7) {
            buffer[out + i + 8] = (buffer[out + i + 8] + rev0[7 - i] * d + rev1[7 - i] * delta2).toInt()
        }
    }

    fun addDeltaFast(time: Int, delta: Int) {
        val off = time.toLong() * factor + offset
        val fixed = (off shr PRE_SHIFT) and 0xFFFFFFFF
        val out = avail + (fixed shr FRAC_BITS).toInt()
        val interp = fixed shr (FRAC_BITS - DELTA_BITS) and (DELTA_UNIT - 1)
        val delta2 = delta * interp

        if (out > size + END_FRAME_EXTRA) {
            throw IllegalStateException("Buffer size was exceeded")
        }

        buffer[out + 7] = (buffer[out + 7] + (delta * DELTA_UNIT - delta2)).toInt()
        buffer[out + 8] = (buffer[out + 8] + delta2).toInt()
    }

    companion object {

        const val PRE_SHIFT = 31 // 32 = unsigned long, 31 = long (Java), 0 = unsigned int.
        const val TIME_BITS = PRE_SHIFT + 20
        const val TIME_UNIT = 1L shl TIME_BITS
        const val BASS_SHIFT = 9
        const val END_FRAME_EXTRA = 2
        const val HALF_WIDTH = 8
        const val BUF_EXTRA = HALF_WIDTH * 2 + END_FRAME_EXTRA
        const val PHASE_BITS = 5
        const val PHASE_COUNT = 1 shl PHASE_BITS
        const val DELTA_BITS = 15
        const val DELTA_UNIT = 1L shl DELTA_BITS
        const val FRAC_BITS = TIME_BITS - PRE_SHIFT
        const val PHASE_SHIFT = FRAC_BITS - PHASE_BITS
        const val BLIP_MAX_RATIO = 1 shl 20
        const val MAX_SAMPLE = 32767
        const val MIN_SAMPLE = -32768
        const val BLIP_MAX_FRAME = 4000

        private val BL_STEP = arrayOf(
            shortArrayOf(43, -115, 350, -488, 1136, -914, 5861, 21022),
            shortArrayOf(44, -118, 348, -473, 1076, -799, 5274, 21001),
            shortArrayOf(45, -121, 344, -454, 1011, -677, 4706, 20936),
            shortArrayOf(46, -122, 336, -431, 942, -549, 4156, 20829),
            shortArrayOf(47, -123, 327, -404, 868, -418, 3629, 20679),
            shortArrayOf(47, -122, 316, -375, 792, -285, 3124, 20488),
            shortArrayOf(47, -120, 303, -344, 714, -151, 2644, 20256),
            shortArrayOf(46, -117, 289, -310, 634, -17, 2188, 19985),
            shortArrayOf(46, -114, 273, -275, 553, 117, 1758, 19675),
            shortArrayOf(44, -108, 255, -237, 471, 247, 1356, 19327),
            shortArrayOf(43, -103, 237, -199, 390, 373, 981, 18944),
            shortArrayOf(42, -98, 218, -160, 310, 495, 633, 18527),
            shortArrayOf(40, -91, 198, -121, 231, 611, 314, 18078),
            shortArrayOf(38, -84, 178, -81, 153, 722, 22, 17599),
            shortArrayOf(36, -76, 157, -43, 80, 824, -241, 17092),
            shortArrayOf(34, -68, 135, -3, 8, 919, -476, 16558),
            shortArrayOf(32, -61, 115, 34, -60, 1006, -683, 16001),
            shortArrayOf(29, -52, 94, 70, -123, 1083, -862, 15422),
            shortArrayOf(27, -44, 73, 106, -184, 1152, -1015, 14824),
            shortArrayOf(25, -36, 53, 139, -239, 1211, -1142, 14210),
            shortArrayOf(22, -27, 34, 170, -290, 1261, -1244, 13582),
            shortArrayOf(20, -20, 16, 199, -335, 1301, -1322, 12942),
            shortArrayOf(18, -12, -3, 226, -375, 1331, -1376, 12293),
            shortArrayOf(15, -4, -19, 250, -410, 1351, -1408, 11638),
            shortArrayOf(13, 3, -35, 272, -439, 1361, -1419, 10979),
            shortArrayOf(11, 9, -49, 292, -464, 1362, -1410, 10319),
            shortArrayOf(9, 16, -63, 309, -483, 1354, -1383, 9660),
            shortArrayOf(7, 22, -75, 322, -496, 1337, -1339, 9005),
            shortArrayOf(6, 26, -85, 333, -504, 1312, -1280, 8355),
            shortArrayOf(4, 31, -94, 341, -507, 1278, -1205, 7713),
            shortArrayOf(3, 35, -102, 347, -506, 1238, -1119, 7082),
            shortArrayOf(1, 40, -110, 350, -499, 1190, -1021, 6464),
            shortArrayOf(0, 43, -115, 350, -488, 1136, -914, 5861),
        )
    }
}
