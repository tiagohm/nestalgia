package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class NoiseChannel(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer,
) : ApuEnvelope(channel, console, mixer) {

    private var modeFlag = false

    // On power-up, the shift register is loaded with the value 1.
    var shiftRegister: UShort = 1U
        private set

    override val frequency: Double
        get() = region.clockRate / (period.toInt() + 1.0) / if (modeFlag) 93 else 1

    inline val isMuted: Boolean
        // The mixer receives the current envelope volume except when Bit 0 of the shift register is set, or The length counter is zero
        get() = shiftRegister.bit0

    override fun clock() {
        // Feedback is calculated as the exclusive-OR of bit 0 and one other bit: bit 6 if Mode flag is set, otherwise bit 1.
        val mode = if (console.settings.checkFlag(EmulationFlag.DISABLE_NOISE_MODEL_FLAG)) false else modeFlag

        val feedback = (shiftRegister and 0x01U) xor ((shiftRegister shr if (mode) 6 else 1) and 0x01U)
        shiftRegister = shiftRegister shr 1
        shiftRegister = shiftRegister or (feedback.toUInt() shl 14).toUShort()

        if (isMuted) {
            addOutput(0)
        } else {
            addOutput(volume.toByte())
        }
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        period =
            ((if (region == Region.NTSC) NOISE_PERIOD_LOOKUP_TABLE_NTSC else NOISE_PERIOD_LOOKUP_TABLE_PAL)[0] - 1U).toUShort()
        shiftRegister = 1U
        modeFlag = false
    }

    override fun getMemoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.WRITE, 0x400CU, 0x400FU)
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        console.apu.run()

        when (addr.toUInt() and 0x03U) {
            // 400C
            0U -> {
                initializeLengthCounter(value.bit5)
                initializeEnvelope(value)
            }
            // 400E
            2U -> {
                period =
                    ((if (region == Region.NTSC) NOISE_PERIOD_LOOKUP_TABLE_NTSC else NOISE_PERIOD_LOOKUP_TABLE_PAL)[value.toInt() and 0x0F] - 1U).toUShort()
                modeFlag = value.bit7
            }
            // 400F
            3U -> {
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

        shiftRegister = s.readUShort("shiftRegister") ?: 1U
        modeFlag = s.readBoolean("modeFlag") ?: false
    }

    companion object {
        val NOISE_PERIOD_LOOKUP_TABLE_NTSC =
            ushortArrayOf(4U, 8U, 16U, 32U, 64U, 96U, 128U, 160U, 202U, 254U, 380U, 508U, 762U, 1016U, 2034U, 4068U)
        val NOISE_PERIOD_LOOKUP_TABLE_PAL =
            ushortArrayOf(4U, 8U, 14U, 30U, 60U, 88U, 118U, 148U, 188U, 236U, 354U, 472U, 708U, 944U, 1890U, 3778U)
    }
}