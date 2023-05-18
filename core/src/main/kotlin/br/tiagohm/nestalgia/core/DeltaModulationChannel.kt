package br.tiagohm.nestalgia.core

import kotlin.math.abs

@Suppress("NOTHING_TO_INLINE")
class DeltaModulationChannel(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer? = null,
) : ApuChannel(channel, console, mixer) {

    private var sampleLength: UShort = 0U
    private var outputLevel: UByte = 0U
    private var irqEnabled = false
    private var loop = false

    private var bytesRemaining: UShort = 0U
    private var readBuffer: UByte = 0U
    private var bufferEmpty = true

    private var shiftRegister: UByte = 0U
    private var bitsRemaining: UByte = 0U
    private var silence = true
    private var isNeedToRun = false
    private var needInit: UByte = 0U

    private var lastValue4011: UByte = 0U

    var sampleAddr: UShort = 0U
        private set

    var dmcReadAddress: UShort = 0U
        private set

    override val frequency: Double
        get() = region.clockRate / (period.toInt() + 1.0)

    override val volume: Long
        get() = lastOutput.toLong()

    override val isEnabled: Boolean
        get() = irqEnabled

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        if (!softReset) {
            // At power on, the sample address is set to $C000 and the sample length is set to 1
            // Resetting does not reset their value
            sampleAddr = 0xC000U
            sampleLength = 1U
        }

        outputLevel = 0U
        irqEnabled = false
        loop = false

        dmcReadAddress = 0U
        bytesRemaining = 0U
        readBuffer = 0U
        bufferEmpty = true

        shiftRegister = 0U
        bitsRemaining = 8U
        silence = true
        isNeedToRun = false

        lastValue4011 = 0U

        // Not sure if this is accurate, but it seems to make things better rather than worse (for dpcmletterbox)
        // On the real thing, I think the power-on value is 428 (or the equivalent at least - it uses a linear feedback shift register), though only the even/oddness should matter for this test.
        period =
            ((if (region == Region.NTSC) DMC_PERIOD_LOOKUP_TABLE_NTSC else DMC_PERIOD_LOOKUP_TABLE_PAL)[0] - 1).toUShort()

        // Make sure the DMC doesn't tick on the first cycle - this is part of what keeps Sprite/DMC DMA tests working while fixing dmc_pitch.
        timer = period
    }

    private inline fun initSample() {
        dmcReadAddress = sampleAddr
        bytesRemaining = sampleLength
        isNeedToRun = bytesRemaining > 0U
    }

    private inline fun startDmcTransfer() {
        if (bufferEmpty && bytesRemaining > 0U) {
            console.cpu.startDmcTransfer()
        }
    }

    fun setDmcReadBuffer(value: UByte) {
        if (bytesRemaining > 0U) {
            readBuffer = value
            bufferEmpty = false

            // The address is incremented; if it exceeds $FFFF, it is wrapped around to $8000.
            dmcReadAddress++

            if (dmcReadAddress.toUInt() == 0U) {
                dmcReadAddress = 0x8000U
            }

            bytesRemaining--

            if (bytesRemaining.toUInt() == 0U) {
                isNeedToRun = false

                if (loop) {
                    // Looped sample should never set IRQ flag
                    initSample()
                } else if (irqEnabled) {
                    console.cpu.setIRQSource(IRQSource.DMC)
                }
            }
        }
    }

    override fun clock() {
        if (!silence) {
            if (shiftRegister.bit0) {
                if (outputLevel <= 125U) {
                    outputLevel++
                    outputLevel++
                }
            } else {
                if (outputLevel >= 2U) {
                    outputLevel--
                    outputLevel--
                }
            }

            shiftRegister = shiftRegister shr 1
        }

        bitsRemaining--

        if (bitsRemaining.isZero) {
            bitsRemaining = 8U

            if (bufferEmpty) {
                silence = true
            } else {
                silence = false
                shiftRegister = readBuffer
                bufferEmpty = true
                startDmcTransfer()
            }
        }

        addOutput(outputLevel.toByte())
    }

    fun irqPending(cyclesToRun: Int): Boolean {
        if (irqEnabled && bytesRemaining > 0U) {
            val cyclesToEmptyBuffer = (bitsRemaining + (bytesRemaining - 1U) * 8U) * period

            if (cyclesToRun >= cyclesToEmptyBuffer.toInt()) {
                return true
            }
        }

        return false
    }

    override val status: Boolean
        get() = bytesRemaining > 0U

    override fun getMemoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.WRITE, 0x4010U, 0x4013U)
    }

    fun needToRun(): Boolean {
        if (needInit > 0U) {
            needInit--

            if (needInit.isZero) {
                startDmcTransfer()
            }
        }

        return isNeedToRun
    }

    fun setEnabled(enabled: Boolean) {
        if (!enabled) {
            bytesRemaining = 0U
            isNeedToRun = false
        } else if (bytesRemaining.isZero) {
            initSample()

            // Delay a number of cycles based on odd/even cycles
            // Allows behavior to match dmc_dma_start_test
            needInit = if ((console.cpu.cycleCount and 0x01L) == 0L) {
                2U
            } else {
                3U
            }
        }
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        console.apu.run()

        when (addr.toUInt() and 0x03U) {
            // 4010
            0U -> {
                irqEnabled = value.bit7
                loop = value.bit6

                // The rate determines for how many CPU cycles happen between changes in the output level during automatic delta-encoded sample playback.
                // Because BaseApuChannel does not decrement when setting _timer, we need to actually set the value to 1 less than the lookup table
                period =
                    ((if (region == Region.NTSC) DMC_PERIOD_LOOKUP_TABLE_NTSC else DMC_PERIOD_LOOKUP_TABLE_PAL)[value.toInt() and 0x0F] - 1).toUShort()

                if (!irqEnabled) {
                    console.cpu.clearIRQSource(IRQSource.DMC)
                }
            }
            // 4011
            1U -> {
                val newValue = value and 0x7FU
                val previousLevel = outputLevel
                outputLevel = newValue

                if (console.settings.checkFlag(EmulationFlag.REDUCE_DMC_POPPING) && abs(outputLevel.toInt() - previousLevel.toInt()) > 50) {
                    // Reduce popping sounds for 4011 writes
                    outputLevel = (outputLevel.toInt() - (outputLevel.toInt() - previousLevel.toInt()) / 2).toUByte()
                }

                // 4011 applies new output right away, not on the timer's reload.  This fixes bad DMC sound when playing through 4011.
                addOutput(outputLevel.toByte())

                if (lastValue4011 != value && newValue > 0U) {
                    console.setNextFrameOverclockStatus(true)
                }

                lastValue4011 = newValue
            }
            // 4012
            2U -> {
                sampleAddr = (0xC000U or (value.toUInt() shl 6)).toUShort()

                if (value > 0U) {
                    console.setNextFrameOverclockStatus(false)
                }
            }
            // 4013
            3U -> {
                sampleLength = ((value.toUInt() shl 4) or 0x0001U).toUShort()

                if (value > 0U) {
                    console.setNextFrameOverclockStatus(false)
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("sampleAddr", sampleAddr)
        s.write("sampleLength", sampleLength)
        s.write("outputLevel", outputLevel)
        s.write("irqEnabled", irqEnabled)
        s.write("loop", loop)
        s.write("dmcReadAddress", dmcReadAddress)
        s.write("bytesRemaining", bytesRemaining)
        s.write("readBuffer", readBuffer)
        s.write("bufferEmpty", bufferEmpty)
        s.write("shiftRegister", shiftRegister)
        s.write("bitsRemaining", bitsRemaining)
        s.write("silence", silence)
        s.write("isNeedToRun", isNeedToRun)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        sampleAddr = s.readUShort("sampleAddr") ?: 0U
        sampleLength = s.readUShort("sampleLength") ?: 0U
        outputLevel = s.readUByte("outputLevel") ?: 0U
        irqEnabled = s.readBoolean("irqEnabled") ?: false
        loop = s.readBoolean("loop") ?: false
        dmcReadAddress = s.readUShort("dmcReadAddress") ?: 0U
        bytesRemaining = s.readUShort("bytesRemaining") ?: 0U
        readBuffer = s.readUByte("readBuffer") ?: 0U
        bufferEmpty = s.readBoolean("bufferEmpty") ?: true
        shiftRegister = s.readUByte("shiftRegister") ?: 0U
        bitsRemaining = s.readUByte("bitsRemaining") ?: 0U
        silence = s.readBoolean("silence") ?: true
        isNeedToRun = s.readBoolean("isNeedToRun") ?: false
    }

    companion object {

        @JvmStatic private val DMC_PERIOD_LOOKUP_TABLE_NTSC =
            shortArrayOf(428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54)

        @JvmStatic private val DMC_PERIOD_LOOKUP_TABLE_PAL =
            shortArrayOf(398, 354, 316, 298, 276, 236, 210, 198, 176, 148, 132, 118, 98, 78, 66, 50)
    }
}
