package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.AudioChannel.NAMCO_163

// https://www.nesdev.org/wiki/Namco_163_audio

class Namco163Audio(console: Console) : ExpansionAudio(console), Memory {

    @JvmField internal val internalRam = IntArray(RAM_SIZE)
    private val channelOutput = IntArray(8)
    @Volatile private var ramPosition = 0
    @Volatile private var autoIncrement = false
    @Volatile private var updateCounter = 0
    @Volatile private var currentChannel = 7
    @Volatile private var lastOutput = 0
    @Volatile private var disableSound = false

    @Suppress("NOTHING_TO_INLINE")
    private inline fun frequency(channel: Int): Int {
        val addr = 0x40 + channel * 0x08
        return internalRam[addr + FREQUENCY_HIGH] and 0x03 shl 16 or
            (internalRam[addr + FREQUENCY_MID] shl 8) or
            internalRam[addr + FREQUENCY_LOW]
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun phase(channel: Int): Int {
        val addr = 0x40 + channel * 0x08
        return internalRam[addr + PHASE_HIGH] shl 16 or
            (internalRam[addr + PHASE_MID] shl 8) or
            internalRam[addr + PHASE_LOW]
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun phase(channel: Int, phase: Int) {
        val addr = 0x40 + channel * 0x08
        internalRam[addr + PHASE_HIGH] = phase shr 16 and 0xFF
        internalRam[addr + PHASE_MID] = phase shr 8 and 0xFF
        internalRam[addr + PHASE_LOW] = phase and 0xFF
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun waveAddress(channel: Int): Int {
        val addr = 0x40 + channel * 0x08
        return internalRam[addr + WAVE_ADDRESS]
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun waveLength(channel: Int): Int {
        val addr = 0x40 + channel * 0x08
        return 256 - (internalRam[addr + WAVE_LENGTH] and 0xFC)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun volume(channel: Int): Int {
        val addr = 0x40 + channel * 0x08
        return internalRam[addr + VOLUME] and 0x0F
    }

    private inline val numberOfChannels
        get() = internalRam[0x7F] shr 4 and 0x07

    private fun updateChannel(channel: Int) {
        var phase = phase(channel)
        val freq = frequency(channel)
        val length = waveLength(channel)
        val offset = waveAddress(channel)
        val volume = volume(channel)
        phase = (phase + freq) % (length shl 16)
        val samplePosition = (phase shr 16) + offset and 0xFF

        val sample = if (samplePosition.bit0) internalRam[samplePosition / 2] shr 4
        else internalRam[samplePosition / 2] and 0x0F

        channelOutput[channel] = (sample - 8) * volume
        updateOutputLevel()
        phase(channel, phase)
    }

    private fun updateOutputLevel() {
        var summedOutput = 0
        val min = 7 - numberOfChannels

        for (i in 7 downTo min) {
            summedOutput += channelOutput[i]
        }

        summedOutput /= numberOfChannels + 1
        console.apu.addExpansionAudioDelta(NAMCO_163, summedOutput - lastOutput)
        lastOutput = summedOutput
    }

    internal fun initializeInternalRam(hasBattery: Boolean) {
        if (!hasBattery) {
            console.initializeRam(internalRam)
        }
    }

    override fun clockAudio() {
        if (!disableSound) {
            updateCounter++

            if (updateCounter == 15) {
                updateChannel(currentChannel)

                updateCounter = 0
                currentChannel--

                if (currentChannel < 7 - numberOfChannels) {
                    currentChannel = 7
                }
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun autoIncrement() {
        if (autoIncrement) {
            ramPosition = ramPosition + 1 and 0x7F
        }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return when (addr and 0xF800) {
            0x4800 -> internalRam[ramPosition].also { autoIncrement() }
            else -> 0
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        when (addr and 0xF800) {
            0x4800 -> {
                internalRam[ramPosition] = value
                autoIncrement()
            }
            0xE000 -> disableSound = value.bit6
            0xF800 -> {
                ramPosition = value and 0x7F
                autoIncrement = value.bit7
            }
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("internalRam", internalRam)
        s.write("channelOutput", channelOutput)
        s.write("ramPosition", ramPosition)
        s.write("autoIncrement", autoIncrement)
        s.write("updateCounter", updateCounter)
        s.write("currentChannel", currentChannel)
        s.write("lastOutput", lastOutput)
        s.write("disableSound", disableSound)
    }

    override fun restoreState(s: Snapshot) {
        s.readIntArray("internalRam", internalRam)
        s.readIntArray("channelOutput", channelOutput)
        ramPosition = s.readInt("ramPosition")
        autoIncrement = s.readBoolean("autoIncrement")
        updateCounter = s.readInt("updateCounter")
        currentChannel = s.readInt("currentChannel", 7)
        lastOutput = s.readInt("lastOutput")
        disableSound = s.readBoolean("disableSound")
    }

    companion object {

        const val RAM_SIZE = 0x80

        const val FREQUENCY_LOW = 0x00
        const val PHASE_LOW = 0x01
        const val FREQUENCY_MID = 0x02
        const val PHASE_MID = 0x03
        const val FREQUENCY_HIGH = 0x04
        const val WAVE_LENGTH = 0x04
        const val PHASE_HIGH = 0x05
        const val WAVE_ADDRESS = 0x06
        const val VOLUME = 0x07
    }
}
