package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
open class SquareChannel(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer?,
    val isChannel1: Boolean,
    val isMmc5: Boolean = false,
) : ApuEnvelope(channel, console, mixer) {

    var duty: UByte = 0U
        protected set
    var dutyPos: UByte = 0U
        protected set

    protected var sweepEnabled = false
    protected var sweepPeriod: UByte = 0U
    protected var sweepNegate = false
    protected var sweepShift: UByte = 0U
    protected var reloadSweep = false
    protected var sweepDivider: UByte = 0U
    protected var sweepTargetPeriod = 0U
    protected var realPeriod: UShort = 0U

    override val frequency: Double
        get() = region.clockRate / 16.0 / (realPeriod.toInt() + 1)

    override fun clock() {
        dutyPos = ((dutyPos - 1U) and 0x07U).toUByte()
        updateOutput()
    }

    val isMuted: Boolean
        get() {
            // A period of t < 8, either set explicitly or via a sweep period update, silences the corresponding pulse channel.
            return realPeriod < 8U || (!sweepNegate && sweepTargetPeriod > 0x7FFU)
        }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        duty = 0U
        dutyPos = 0U

        realPeriod = 0U

        sweepEnabled = false
        sweepPeriod = 0U
        sweepNegate = false
        sweepShift = 0U
        reloadSweep = false
        sweepDivider = 0U
        sweepTargetPeriod = 0U

        updateTargetPeriod()
    }

    open fun updateTargetPeriod() {
        val shiftResult = realPeriod shr sweepShift.toInt()

        if (sweepNegate) {
            sweepTargetPeriod = realPeriod - shiftResult

            if (isChannel1) {
                // As a result, a negative sweep on pulse channel 1 will subtract the shifted period value minus 1
                sweepTargetPeriod--
            }
        } else {
            sweepTargetPeriod = realPeriod + shiftResult
        }
    }

    override fun getMemoryRanges(ranges: MemoryRanges) {
        if (isChannel1) {
            ranges.addHandler(MemoryOperation.WRITE, 0x4000U, 0x4003U)
        } else {
            ranges.addHandler(MemoryOperation.WRITE, 0x4004U, 0x4007U)
        }
    }

    open fun initializeSweep(value: UByte) {
        sweepEnabled = value.bit7
        sweepNegate = value.bit3

        // The divider's period is set to P + 1
        sweepPeriod = (((value and 0x70U) shr 4) + 1U).toUByte()
        sweepShift = value and 0x07U

        updateTargetPeriod()

        // Side effects: Sets the reload flag
        reloadSweep = true
    }

    inline fun updateOutput() {
        if (isMuted) {
            addOutput(0)
        } else {
            addOutput((DUTY_SEQUENCES[duty.toInt()][dutyPos.toInt()].toInt() * volume).toByte())
        }
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        console.apu.run()

        when ((addr and 0x03U).toUInt()) {
            // 4000/4004
            0U -> {
                initializeLengthCounter(value.bit5)
                initializeEnvelope(value)

                duty = (value and 0xC0U) shr 6

                if (console.settings.checkFlag(EmulationFlag.SWAP_DUTY_CYCLES)) {
                    duty = ((if (duty.bit1) 0x01U else 0x00U) or (if (duty.bit0) 0x02U else 0x00U)).toUByte()
                }
            }
            // 4001/4005
            1U -> {
                initializeSweep(value)
            }
            // 4002/4006
            2U -> {
                period = ((realPeriod and 0x0700U) or value.toUShort())
            }
            // 4003/4007
            3U -> {
                loadLengthCounter(value shr 3)

                period = (realPeriod and 0xFFU) or ((value.toUInt() and 0x07U) shl 8).toUShort()

                // The sequencer is restarted at the first value of the current sequence.
                dutyPos = 0U

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

        if (sweepDivider.isZero) {
            if (sweepShift > 0U &&
                sweepEnabled &&
                realPeriod >= 8U &&
                sweepTargetPeriod <= 0x7FFU
            ) {
                period = sweepTargetPeriod.toUShort()
            }

            sweepDivider = sweepPeriod
        }

        if (reloadSweep) {
            sweepDivider = sweepPeriod
            reloadSweep = false
        }
    }

    override var period: UShort
        get() = super.period
        set(value) {
            realPeriod = value
            super.period = (realPeriod * 2U + 1U).toUShort()
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

        realPeriod = s.readUShort("realPeriod") ?: 0U
        duty = s.readUByte("duty") ?: 0U
        dutyPos = s.readUByte("dutyPos") ?: 0U
        sweepEnabled = s.readBoolean("sweepEnabled") ?: false
        sweepPeriod = s.readUByte("sweepPeriod") ?: 0U
        sweepNegate = s.readBoolean("sweepNegate") ?: false
        sweepShift = s.readUByte("sweepShift") ?: 0U
        reloadSweep = s.readBoolean("reloadSweep") ?: false
        sweepDivider = s.readUByte("sweepDivider") ?: 0U
        sweepTargetPeriod = s.readUInt("sweepTargetPeriod") ?: 0U
    }

    companion object {

        @JvmStatic val DUTY_SEQUENCES = arrayOf(
            ubyteArrayOf(0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U),
            ubyteArrayOf(0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U),
            ubyteArrayOf(0U, 0U, 0U, 0U, 1U, 1U, 1U, 1U),
            ubyteArrayOf(1U, 1U, 1U, 1U, 1U, 1U, 0U, 0U),
        )
    }
}
