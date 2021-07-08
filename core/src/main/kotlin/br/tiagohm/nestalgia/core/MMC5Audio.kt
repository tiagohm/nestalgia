package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/MMC5_audio

@ExperimentalUnsignedTypes
class MMC5Audio(console: Console) :
    Memory,
    ExpansionAudio(console) {

    private val square1 = MMC5SquareChannel(console)
    private val square2 = MMC5SquareChannel(console)
    private var audioCounter = 0
    private var lastOutput = 0
    private var pcmReadMode = false
    private var pcmIrqEnabled = false
    private var pcmIrqTrip = false
    private var pcmOutput: UByte = 0U

    override fun clockAudio() {
        audioCounter--

        square1.run()
        square2.run()

        if (audioCounter <= 0) {
            // ~240hz envelope/length counter
            audioCounter = console.region.clockRate / 240

            square1.tickLengthCounter()
            square1.tickEnvelope()

            square2.tickLengthCounter()
            square2.tickEnvelope()
        }

        // The sound output of the square channels are equivalent in volume to the corresponding APU channels
        // The polarity of all MMC5 channels is reversed compared to the APU
        val summedOutput = -(square1.output.toInt() + square2.output.toInt() + pcmOutput.toInt())

        if (summedOutput != lastOutput) {
            console.apu.addExpansionAudioDelta(AudioChannel.MMC5, summedOutput - lastOutput)
            lastOutput = summedOutput
        }

        square1.reloadCounter()
        square2.reloadCounter()
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        return when (addr.toInt()) {
            0x5010 -> {
                var status: UByte = 0U
                status = status or if (pcmReadMode) 0x01U else 0x00U
                status = status or if (pcmIrqTrip and pcmIrqEnabled) 0x80U else 0x00U

                // TODO: PCM IRQ
                pcmIrqTrip = false

                status
            }
            0x5015 -> {
                var status: UByte = 0U
                status = status or if (square1.status) 0x01U else 0x00U
                status = status or if (square2.status) 0x02U else 0x00U
                status
            }
            else -> console.memoryManager.getOpenBus()
        }
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        when (addr.toInt()) {
            0x5000, 0x5001, 0x5002, 0x5003 -> square1.write(addr, value, type)
            0x5004, 0x5005, 0x5006, 0x5007 -> square2.write(addr, value, type)
            0x5010 -> {
                // TODO: Read mode & PCM IRQs are not implemented
                pcmReadMode = value.bit0
                pcmIrqEnabled = value.bit7
            }
            // Shin 4 Nin Uchi Mahjong is the only game to uses the extra PCM channel ($5011).
            0x5011 -> if (!pcmReadMode) if (value.isZero) pcmIrqTrip = true else pcmOutput = value
            0x5015 -> {
                square1.isEnabled = value.bit0
                square2.isEnabled = value.bit1
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("square1", square1)
        s.write("square2", square2)
        s.write("audioCounter", audioCounter)
        s.write("lastOutput", lastOutput)
        s.write("pcmReadMode", pcmReadMode)
        s.write("pcmIrqEnabled", pcmIrqEnabled)
        s.write("pcmOutput", pcmOutput)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshot("square1")?.let { square1.restoreState(it) } ?: square1.reset(false)
        s.readSnapshot("square2")?.let { square2.restoreState(it) } ?: square2.reset(false)
        audioCounter = s.readInt("audioCounter") ?: 0
        lastOutput = s.readInt("lastOutput") ?: 0
        pcmReadMode = s.readBoolean("pcmReadMode") ?: false
        pcmIrqEnabled = s.readBoolean("pcmIrqEnabled") ?: false
        pcmOutput = s.readUByte("pcmOutput") ?: 0U
    }
}