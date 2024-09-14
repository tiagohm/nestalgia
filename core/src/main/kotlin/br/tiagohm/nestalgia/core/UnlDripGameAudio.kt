package br.tiagohm.nestalgia.core

class UnlDripGameAudio(console: Console) : ExpansionAudio(console), Writable, Readable, Resetable {

    private val buffer = IntArray(256)

    @Volatile private var readPos = 0
    @Volatile private var writePos = 0
    @Volatile private var bufferFull = false
    @Volatile private var bufferEmpty = true

    @Volatile private var freq = 0
    @Volatile private var timer = 0
    @Volatile private var volume = 0
    @Volatile private var prevOutput = 0

    override fun clockAudio() {
        if (bufferEmpty) {
            return
        }

        timer--

        if (timer == 0) {
            // Each time the timer reaches zero, it is reloaded and a byte is removed from the
            // channel's FIFO and is output (with 0x80 being the 'center' voltage) at the
            // channel's specified volume.
            timer = freq

            if (readPos == writePos) {
                bufferFull = false
            }

            readPos = (readPos + 1) and 0xFF
            output((buffer[readPos].toByte().toInt() - 0x80) * volume)

            if (readPos == writePos) {
                bufferEmpty = true
            }
        }
    }

    override fun reset(softReset: Boolean) {
        buffer.fill(0)
        readPos = 0
        writePos = 0
        bufferFull = false
        bufferEmpty = true
    }

    private fun output(value: Int) {
        console.apu.addExpansionAudioDelta(AudioChannel.VRC7, (value - prevOutput) * 3)
        prevOutput = value
    }

    override fun read(addr: Int): Int {
        var result = 0

        if (bufferFull) {
            result = result or 0x80
        }
        if (bufferEmpty) {
            result = result or 0x40
        }

        return result
    }

    override fun write(addr: Int, value: Int) {
        when (addr and 0x03) {
            0 -> {
                // Writing any value will silence the corresponding sound channel
                // When a channel's Clear FIFO register is written to, its timer is reset to the
                // last written frequency and it is silenced, outputting a 'center' voltage.
                reset()
                output(0)
                timer = freq
            }
            1 -> {
                // Writing a value will insert it into the FIFO.
                if (readPos == writePos) {
                    // When data is written to an empty channel's Data Port, the channel's timer is
                    // reloaded from the Period registers and playback begins immediately.
                    bufferEmpty = false
                    output((value.toByte().toInt() - 0x80) * volume)
                    timer = freq
                }

                buffer[writePos] = value and 0xFF
                writePos = (writePos + 1) and 0xFF

                if (readPos == writePos) {
                    bufferFull = true
                }
            }
            2 -> {
                // Specifies channel playback rate, in cycles per sample (lower 8 bits)
                freq = (freq and 0x0F00) or value
            }
            else -> {
                // Specifies channel playback rate, in cycles per sample (higher 8 bits) (bits 0-3)
                // Specifies channel playback volume (bits 4-7)
                freq = (freq and 0xFF) or ((value and 0x0F) shl 8)
                volume = (value and 0xF0) shr 4

                if (!bufferEmpty) {
                    // Updates to a channel's Period do not take effect until the current
                    // sample has finished playing, but updates to a channel's Volume take effect immediately.
                    output((buffer[readPos].toByte().toInt() - 0x80) * volume)
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("buffer", buffer)
        s.write("readPos", readPos)
        s.write("writePos", writePos)
        s.write("bufferFull", bufferFull)
        s.write("bufferEmpty", bufferEmpty)

        s.write("freq", freq)
        s.write("timer", timer)
        s.write("volume", volume)
        s.write("prevOutput", prevOutput)
    }

    override fun restoreState(s: Snapshot) {
        s.readIntArray("buffer", buffer)
        readPos = s.readInt("readPos")
        writePos = s.readInt("writePos")
        bufferFull = s.readBoolean("bufferFull")
        bufferEmpty = s.readBoolean("bufferEmpty")

        freq = s.readInt("freq")
        timer = s.readInt("timer")
        volume = s.readInt("volume")
        prevOutput = s.readInt("prevOutput")
    }
}
