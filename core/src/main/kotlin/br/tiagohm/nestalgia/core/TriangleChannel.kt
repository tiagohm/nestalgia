package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

class TriangleChannel(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer,
) : ApuLengthCounter(channel, console, mixer) {

    @Volatile private var linearCounter = 0
    @Volatile private var linearCounterReload = 0
    @Volatile private var linearReloadFlag = false
    @Volatile private var linearControlFlag = false

    var sequencePosition = 0
        private set

    override val frequency
        get() = region.clockRate / 32.0 / (period + 1)

    override val volume
        get() = lastOutput

    override val isMuted
        get() = false

    override fun clock() {
        // The sequencer is clocked by the timer as long as both the linear counter and the length counter are nonzero.
        if (lengthCounter > 0 && linearCounter > 0) {
            sequencePosition = (sequencePosition + 1) and 0x1F

            if (period >= 2 || console.settings.flag(EmulationFlag.SILENCE_TRIANGLE_HIGH_FREQ)) {
                // Disabling the triangle channel when period is < 2 removes "pops" in the audio that are caused by the ultrasonic frequencies
                // This is less "accurate" in terms of emulation, so this is an option (disabled by default)
                addOutput(SEQUENCE[sequencePosition])
            }
        }
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        linearCounter = 0
        linearCounterReload = 0
        linearReloadFlag = false
        linearControlFlag = false
        sequencePosition = 0
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(WRITE, 0x4008, 0x400B)
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        console.apu.run()

        when (addr.toUInt() and 0x03U) {
            // 4008
            0U -> {
                linearControlFlag = value.bit7
                linearCounterReload = value and 0x7F
                initializeLengthCounter(linearControlFlag)
            }
            // 400A
            2U -> {
                period = (period and 0xFF00) or value
            }
            // 400B
            3U -> {
                loadLengthCounter(value shr 3)

                period = period and 0x00FF
                period = period or ((value and 0x07) shl 8)

                // Side effects: Sets the linear counter reload flag.
                linearReloadFlag = true
            }
        }
    }

    fun tickLinearCounter() {
        if (linearReloadFlag) {
            linearCounter = linearCounterReload
        } else if (linearCounter > 0) {
            linearCounter--
        }

        if (!linearControlFlag) {
            linearReloadFlag = false
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("linearCounter", linearCounter)
        s.write("linearCounterReload", linearCounterReload)
        s.write("linearReloadFlag", linearReloadFlag)
        s.write("linearControlFlag", linearControlFlag)
        s.write("sequencePosition", sequencePosition)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        linearCounter = s.readInt("linearCounter")
        linearCounterReload = s.readInt("linearCounterReload")
        linearReloadFlag = s.readBoolean("linearReloadFlag")
        linearControlFlag = s.readBoolean("linearControlFlag")
        sequencePosition = s.readInt("sequencePosition")
    }

    companion object {

        private val SEQUENCE = intArrayOf(
            15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        )
    }
}
