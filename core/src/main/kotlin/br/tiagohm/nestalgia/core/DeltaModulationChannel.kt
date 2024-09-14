package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import kotlin.math.abs

class DeltaModulationChannel(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer? = null,
) : ApuChannel(channel, console, mixer) {

    @Volatile private var sampleLength = 0
    @Volatile private var outputLevel = 0
    @Volatile private var irqEnabled = false
    @Volatile private var loop = false

    @Volatile private var bytesRemaining = 0
    @Volatile private var readBuffer = 0
    @Volatile private var bufferEmpty = true

    @Volatile private var shiftRegister = 0
    @Volatile private var bitsRemaining = 0
    @Volatile private var silence = true
    @Volatile private var needToRun = false
    @Volatile private var needInit = 0

    @Volatile private var lastValue4011 = 0

    var sampleAddr = 0
        private set

    var dmcReadAddress = 0
        private set

    override val frequency
        get() = region.clockRate / (period + 1.0)

    override val volume
        get() = lastOutput

    override val enabled
        get() = irqEnabled

    override val isMuted
        get() = false

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        if (!softReset) {
            // At power on, the sample address is set to $C000 and the sample length is set to 1
            // Resetting does not reset their value
            sampleAddr = 0xC000
            sampleLength = 1
        }

        outputLevel = 0
        irqEnabled = false
        loop = false

        dmcReadAddress = 0
        bytesRemaining = 0
        readBuffer = 0
        bufferEmpty = true

        shiftRegister = 0
        bitsRemaining = 8
        silence = true
        needToRun = false

        lastValue4011 = 0

        // Not sure if this is accurate, but it seems to make things better rather than worse (for dpcmletterbox)
        // On the real thing, I think the power-on value is 428 (or the equivalent at least - it uses a linear feedback shift register), though only the even/oddness should matter for this test.
        period = (if (region == Region.NTSC) DMC_PERIOD_LOOKUP_TABLE_NTSC else DMC_PERIOD_LOOKUP_TABLE_PAL)[0] - 1

        // Make sure the DMC doesn't tick on the first cycle - this is part of what keeps Sprite/DMC DMA tests working while fixing dmc_pitch.
        timer = period
    }

    private fun initSample() {
        dmcReadAddress = sampleAddr
        bytesRemaining = sampleLength
        needToRun = bytesRemaining > 0
    }

    private fun startDmcTransfer() {
        if (bufferEmpty && bytesRemaining > 0) {
            console.cpu.startDmcTransfer()
        }
    }

    fun dmcReadBuffer(value: Int) {
        if (bytesRemaining > 0) {
            readBuffer = value
            bufferEmpty = false

            // The address is incremented; if it exceeds $FFFF, it is wrapped around to $8000.
            dmcReadAddress = (dmcReadAddress + 1) and 0xFFFF

            if (dmcReadAddress == 0) {
                dmcReadAddress = 0x8000
            }

            bytesRemaining--

            if (bytesRemaining <= 0) {
                needToRun = false

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
                if (outputLevel <= 125) {
                    outputLevel++
                    outputLevel++
                }
            } else {
                if (outputLevel >= 2) {
                    outputLevel--
                    outputLevel--
                }
            }

            shiftRegister = shiftRegister shr 1
        }

        bitsRemaining--

        if (bitsRemaining <= 0) {
            bitsRemaining = 8

            if (bufferEmpty) {
                silence = true
            } else {
                silence = false
                shiftRegister = readBuffer
                bufferEmpty = true
                startDmcTransfer()
            }
        }

        addOutput(outputLevel)
    }

    fun irqPending(cyclesToRun: Int): Boolean {
        if (irqEnabled && bytesRemaining > 0) {
            val cyclesToEmptyBuffer = (bitsRemaining + (bytesRemaining - 1) * 8) * period

            if (cyclesToRun >= cyclesToEmptyBuffer) {
                return true
            }
        }

        return false
    }

    override val status
        get() = bytesRemaining > 0

    override fun memoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(WRITE, 0x4010, 0x4013)
    }

    fun needToRun(): Boolean {
        if (needInit > 0) {
            needInit--

            if (needInit == 0) {
                startDmcTransfer()
            }
        }

        return needToRun
    }

    fun enable(enabled: Boolean) {
        if (!enabled) {
            bytesRemaining = 0
            needToRun = false
        } else if (bytesRemaining <= 0) {
            initSample()

            // Delay a number of cycles based on odd/even cycles
            // Allows behavior to match dmc_dma_start_test.
            needInit = if (console.cpu.cycleCount.bit0) 3 else 2
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        console.apu.run()

        when (addr and 0x03) {
            // 4010
            0 -> {
                irqEnabled = value.bit7
                loop = value.bit6

                // The rate determines for how many CPU cycles happen between changes in the output level during automatic delta-encoded sample playback.
                // Because BaseApuChannel does not decrement when setting _timer, we need to actually set the value to 1 less than the lookup table
                period = (if (region == Region.NTSC) DMC_PERIOD_LOOKUP_TABLE_NTSC else DMC_PERIOD_LOOKUP_TABLE_PAL)[value and 0x0F] - 1

                if (!irqEnabled) {
                    console.cpu.clearIRQSource(IRQSource.DMC)
                }
            }
            // 4011
            1 -> {
                val newValue = value and 0x7F
                val previousLevel = outputLevel
                outputLevel = newValue

                if (console.settings.flag(EmulationFlag.REDUCE_DMC_POPPING) && abs(outputLevel - previousLevel) > 50) {
                    // Reduce popping sounds for 4011 writes
                    outputLevel -= (outputLevel - previousLevel) / 2
                }

                // 4011 applies new output right away, not on the timer's reload.  This fixes bad DMC sound when playing through 4011.
                addOutput(outputLevel)

                if (lastValue4011 != value && newValue > 0) {
                    console.nextFrameOverclockStatus(true)
                }

                lastValue4011 = newValue
            }
            // 4012
            2 -> {
                sampleAddr = 0xC000 or (value shl 6)

                if (value > 0) {
                    console.nextFrameOverclockStatus(false)
                }
            }
            // 4013
            3 -> {
                sampleLength = (value shl 4) or 0x0001

                if (value > 0) {
                    console.nextFrameOverclockStatus(false)
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
        s.write("needToRun", needToRun)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        sampleAddr = s.readInt("sampleAddr")
        sampleLength = s.readInt("sampleLength")
        outputLevel = s.readInt("outputLevel")
        irqEnabled = s.readBoolean("irqEnabled")
        loop = s.readBoolean("loop")
        dmcReadAddress = s.readInt("dmcReadAddress")
        bytesRemaining = s.readInt("bytesRemaining")
        readBuffer = s.readInt("readBuffer")
        bufferEmpty = s.readBoolean("bufferEmpty", true)
        shiftRegister = s.readInt("shiftRegister")
        bitsRemaining = s.readInt("bitsRemaining")
        silence = s.readBoolean("silence", true)
        needToRun = s.readBoolean("needToRun")
    }

    companion object {

        private val DMC_PERIOD_LOOKUP_TABLE_NTSC =
            shortArrayOf(428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54)

        private val DMC_PERIOD_LOOKUP_TABLE_PAL =
            shortArrayOf(398, 354, 316, 298, 276, 236, 210, 198, 176, 148, 132, 118, 98, 78, 66, 50)
    }
}
