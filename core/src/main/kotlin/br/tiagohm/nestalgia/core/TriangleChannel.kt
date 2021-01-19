package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class TriangleChannel(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer,
) : ApuLengthCounter(channel, console, mixer) {

    private var linearCounter: UByte = 0U
    private var linearCounterReload: UByte = 0U
    private var linearReloadFlag = false
    private var linearControlFlag = false

    var sequencePosition: UByte = 0U
        private set

    override val frequency: Double
        get() = region.clockRate / 32.0 / (period.toInt() + 1)

    override val volume: Long
        get() = lastOutput.toLong()

    override fun clock() {
        // The sequencer is clocked by the timer as long as both the linear counter and the length counter are nonzero.
        if (lengthCounter > 0U && linearCounter > 0U) {
            sequencePosition = ((sequencePosition + 1U) and 0x1FU).toUByte()

            if (period >= 2U || console.settings.checkFlag(EmulationFlag.SILENCE_TRIANGLE_HIGH_FREQ)) {
                // Disabling the triangle channel when period is < 2 removes "pops" in the audio that are caused by the ultrasonic frequencies
                // This is less "accurate" in terms of emulation, so this is an option (disabled by default)
                addOutput(SEQUENCE[sequencePosition.toInt()])
            }
        }
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        linearCounter = 0U
        linearCounterReload = 0U
        linearReloadFlag = false
        linearControlFlag = false
        sequencePosition = 0U
    }

    override fun getMemoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.WRITE, 0x4008U, 0x400BU)
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        console.apu.run()

        when (addr.toUInt() and 0x03U) {
            // 4008
            0U -> {
                linearControlFlag = value.bit7
                linearCounterReload = value and 0x7FU
                initializeLengthCounter(linearControlFlag)
            }
            // 400A
            2U -> {
                period = (period and 0xFF00U) or value.toUShort()
            }
            // 400B
            3U -> {
                loadLengthCounter(value shr 3)

                period = period and 0x00FFU
                period = period or ((value.toUInt() and 0x07U) shl 8).toUShort()

                // Side effects: Sets the linear counter reload flag
                linearReloadFlag = true
            }
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte = 0U

    fun tickLinearCounter() {
        if (linearReloadFlag) {
            linearCounter = linearCounterReload
        } else if (linearCounter > 0U) {
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

        linearCounter = s.readUByte("linearCounter") ?: 0U
        linearCounterReload = s.readUByte("linearCounterReload") ?: 0U
        linearReloadFlag = s.readBoolean("linearReloadFlag") ?: false
        linearControlFlag = s.readBoolean("linearControlFlag") ?: false
        sequencePosition = s.readUByte("sequencePosition") ?: 0U
    }

    companion object {
        private val SEQUENCE = byteArrayOf(
            15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        )
    }
}