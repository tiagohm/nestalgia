package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

class NoiseChannel(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer,
) : ApuEnvelope(channel, console, mixer) {

    private var modeFlag = false

    // On power-up, the shift register is loaded with the value 1.
    var shiftRegister = 1
        private set

    override val frequency
        get() = region.clockRate / (period + 1.0) / if (modeFlag) 93 else 1

    override val isMuted
        // The mixer receives the current envelope volume except when Bit 0
        // of the shift register is set, or The length counter is zero.
        get() = shiftRegister.bit0

    override fun clock() {
        // Feedback is calculated as the exclusive-OR of bit 0 and one other bit: bit 6 if Mode flag is set, otherwise bit 1.
        val mode = if (console.settings.flag(EmulationFlag.DISABLE_NOISE_MODE_FLAG)) false else modeFlag

        val feedback = (shiftRegister and 0x01) xor ((shiftRegister shr if (mode) 6 else 1) and 0x01)
        shiftRegister = shiftRegister shr 1
        shiftRegister = shiftRegister or (feedback shl 14)

        if (isMuted) {
            addOutput(0)
        } else {
            addOutput(volume)
        }
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        period = (if (region == Region.NTSC) NOISE_PERIOD_LOOKUP_TABLE_NTSC else NOISE_PERIOD_LOOKUP_TABLE_PAL)[0] - 1
        shiftRegister = 1
        modeFlag = false
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(WRITE, 0x400C, 0x400F)
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        console.apu.run()

        when (addr and 0x03) {
            // 400C
            0 -> {
                initializeLengthCounter(value.bit5)
                initializeEnvelope(value)
            }
            // 400E
            2 -> {
                period = (if (region == Region.NTSC) NOISE_PERIOD_LOOKUP_TABLE_NTSC
                else NOISE_PERIOD_LOOKUP_TABLE_PAL)[value and 0x0F] - 1
                modeFlag = value.bit7
            }
            // 400F
            3 -> {
                loadLengthCounter(value shr 3)
                // The envelope is also restarted.
                resetEnvelope()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("shiftRegister", shiftRegister)
        s.write("modeFlag", modeFlag)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        shiftRegister = s.readInt("shiftRegister", 1)
        modeFlag = s.readBoolean("modeFlag")
    }

    companion object {

        internal val NOISE_PERIOD_LOOKUP_TABLE_NTSC = intArrayOf(4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068)
        internal val NOISE_PERIOD_LOOKUP_TABLE_PAL = intArrayOf(4, 8, 14, 30, 60, 88, 118, 148, 188, 236, 354, 472, 708, 944, 1890, 3778)
    }
}
