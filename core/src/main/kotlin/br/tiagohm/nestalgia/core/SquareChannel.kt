package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

@Suppress("NOTHING_TO_INLINE")
open class SquareChannel(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer?,
    private val channel1: Boolean,
    private val isMmc5: Boolean = false,
) : ApuEnvelope(channel, console, mixer) {

    var duty = 0
        protected set

    var dutyPos = 0
        protected set

    protected var sweepEnabled = false
    protected var sweepPeriod = 0
    protected var sweepNegate = false
    protected var sweepShift = 0
    protected var reloadSweep = false
    protected var sweepDivider = 0
    protected var sweepTargetPeriod = 0
    protected var realPeriod = 0

    override val frequency
        get() = region.clockRate / 16.0 / (realPeriod + 1)

    override fun clock() {
        dutyPos = (dutyPos - 1) and 0x07
        updateOutput()
    }

    override val isMuted
        // A period of t < 8, either set explicitly or via a sweep period update,
        // silences the corresponding pulse channel.
        get() = realPeriod < 8 || (!sweepNegate && sweepTargetPeriod > 0x7FF)

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        duty = 0
        dutyPos = 0

        realPeriod = 0

        sweepEnabled = false
        sweepPeriod = 0
        sweepNegate = false
        sweepShift = 0
        reloadSweep = false
        sweepDivider = 0
        sweepTargetPeriod = 0

        updateTargetPeriod()
    }

    open fun updateTargetPeriod() {
        val shiftResult = realPeriod shr sweepShift

        if (sweepNegate) {
            sweepTargetPeriod = realPeriod - shiftResult

            if (channel1) {
                // As a result, a negative sweep on pulse channel 1 will subtract the shifted period value minus 1
                sweepTargetPeriod--
            }
        } else {
            sweepTargetPeriod = realPeriod + shiftResult
        }
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        if (channel1) {
            ranges.addHandler(WRITE, 0x4000, 0x4003)
        } else {
            ranges.addHandler(WRITE, 0x4004, 0x4007)
        }
    }

    open fun initializeSweep(value: Int) {
        sweepEnabled = value.bit7
        sweepNegate = value.bit3

        // The divider's period is set to P + 1
        sweepPeriod = (value and 0x70 shr 4) + 1
        sweepShift = value and 0x07

        updateTargetPeriod()

        // Side effects: Sets the reload flag.
        reloadSweep = true
    }

    inline fun updateOutput() {
        if (isMuted) {
            addOutput(0)
        } else {
            addOutput(DUTY_SEQUENCES[duty][dutyPos] * volume)
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        console.apu.run()

        when (addr and 0x03) {
            // 4000/4004
            0 -> {
                initializeLengthCounter(value.bit5)
                initializeEnvelope(value)

                duty = (value and 0xC0) shr 6

                if (console.settings.flag(EmulationFlag.SWAP_DUTY_CYCLES)) {
                    duty = (if (duty.bit1) 0x01 else 0x00) or (if (duty.bit0) 0x02 else 0x00)
                }
            }
            // 4001/4005
            1 -> {
                initializeSweep(value)
            }
            // 4002/4006
            2 -> {
                period = (realPeriod and 0x0700) or value
            }
            // 4003/4007
            3 -> {
                loadLengthCounter(value shr 3)

                period = (realPeriod and 0xFF) or (value and 0x07 shl 8)

                // The sequencer is restarted at the first value of the current sequence.
                dutyPos = 0

                //The envelope is also restarted.
                resetEnvelope()
            }
        }

        if (!isMmc5) {
            updateOutput()
        }
    }

    fun tickSweep() {
        sweepDivider--

        if (sweepDivider <= 0) {
            if (sweepShift > 0 &&
                sweepEnabled &&
                realPeriod >= 8 &&
                sweepTargetPeriod <= 0x7FF
            ) {
                period = sweepTargetPeriod
            }

            sweepDivider = sweepPeriod
        }

        if (reloadSweep) {
            sweepDivider = sweepPeriod
            reloadSweep = false
        }
    }

    override var period
        get() = super.period
        set(value) {
            realPeriod = value
            super.period = realPeriod * 2 + 1
            updateTargetPeriod()
        }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("realPeriod", realPeriod)
        s.write("duty", duty)
        s.write("dutyPos", dutyPos)
        s.write("sweepEnabled", sweepEnabled)
        s.write("sweepPeriod", sweepPeriod)
        s.write("sweepNegate", sweepNegate)
        s.write("sweepShift", sweepShift)
        s.write("reloadSweep", reloadSweep)
        s.write("sweepDivider", sweepDivider)
        s.write("sweepTargetPeriod", sweepTargetPeriod)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        realPeriod = s.readInt("realPeriod")
        duty = s.readInt("duty")
        dutyPos = s.readInt("dutyPos")
        sweepEnabled = s.readBoolean("sweepEnabled")
        sweepPeriod = s.readInt("sweepPeriod")
        sweepNegate = s.readBoolean("sweepNegate")
        sweepShift = s.readInt("sweepShift")
        reloadSweep = s.readBoolean("reloadSweep")
        sweepDivider = s.readInt("sweepDivider")
        sweepTargetPeriod = s.readInt("sweepTargetPeriod")
    }

    companion object {

        @PublishedApi internal val DUTY_SEQUENCES = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(0, 0, 0, 0, 0, 0, 1, 1),
            intArrayOf(0, 0, 0, 0, 1, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 0, 0),
        )
    }
}
