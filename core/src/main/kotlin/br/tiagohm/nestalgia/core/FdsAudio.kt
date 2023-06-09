package br.tiagohm.nestalgia.core

import kotlin.math.min

// https://wiki.nesdev.com/w/index.php/FDS_audio

class FdsAudio(console: Console) : ExpansionAudio(console), Memory {

    private val waveTable = IntArray(64)
    private var waveWriteEnabled = false

    private val volume = FdsChannel()
    private val mod = ModulationChannel()

    private var disableEnvelopes = false
    private var haltWaveform = false

    private var masterVolume = 0

    // Internal values
    private var waveOverflowCounter = 0
    private var wavePitch = 0
    private var wavePosition = 0

    private var lastOutput = 0

    override fun clockAudio() {
        val frequency = volume.frequency

        if (!haltWaveform && !disableEnvelopes) {
            volume.tickEnvelope()

            if (mod.tickEnvelope()) {
                mod.updateOutput(frequency)
            }
        }

        if (mod.tickModulator()) {
            // Modulator was ticked, update wave pitch
            mod.updateOutput(frequency)
        }

        if (haltWaveform) {
            wavePosition = 0
            updateOutput()
        } else {
            updateOutput()

            if ((frequency + mod.output) > 0 && !waveWriteEnabled) {
                waveOverflowCounter += frequency + mod.output

                if (waveOverflowCounter < frequency + mod.output) {
                    wavePosition = (wavePosition + 1) and 0x3F
                }
            }
        }
    }

    private fun updateOutput() {
        val level = min(volume.gain, 32) * WAVE_VOLUME_TABLE[masterVolume]
        val outputLevel = (waveTable[wavePosition] * level / 1152) and 0xFF

        if (lastOutput != outputLevel) {
            console.apu.addExpansionAudioDelta(AudioChannel.FDS, outputLevel - lastOutput)
            lastOutput = outputLevel
        }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        var value = console.memoryManager.openBus()

        when {
            addr <= 0x407F -> {
                value = value and 0xC0

                value = if (waveWriteEnabled) {
                    value or waveTable[addr and 0x3F]
                } else {
                    // When writing is disabled ($4089.7), reading anywhere in 4040-407F returns the value at the current wave position
                    value or waveTable[wavePosition]
                }
            }
            addr == 0x4090 -> {
                value = value and 0xC0
                value = value or volume.gain
            }
            addr == 0x4092 -> {
                value = value and 0xC0
                value = value or mod.gain
            }
        }

        return value
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        if (addr <= 0x407F) {
            if (waveWriteEnabled) {
                waveTable[addr and 0x3F] = value and 0x3F
            }
        } else {
            when (addr) {
                0x4080, 0x4082 -> volume.write(addr, value, type)
                0x4083 -> {
                    disableEnvelopes = value.bit6
                    haltWaveform = value.bit7

                    if (disableEnvelopes) {
                        volume.resetTimer()
                        mod.resetTimer()
                    }

                    volume.write(addr, value, type)
                }
                0x4084, 0x4085 -> {
                    mod.write(addr, value, type)
                    // Need to update mod output if gain/speed were changed.
                    mod.updateOutput(volume.frequency)
                }
                0x4086, 0x4087 -> mod.write(addr, value, type)
                0x4088 -> mod.writeModulationTable(value)
                0x4089 -> {
                    masterVolume = value and 0x03
                    waveWriteEnabled = value.bit7
                }
                0x408A -> {
                    volume.masterSpeed = value
                    mod.masterSpeed = value
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("volume", volume)
        s.write("mod", mod)
        s.write("waveWriteEnabled", waveWriteEnabled)
        s.write("disableEnvelopes", disableEnvelopes)
        s.write("haltWaveform", haltWaveform)
        s.write("masterVolume", masterVolume)
        s.write("waveOverflowCounter", waveOverflowCounter)
        s.write("wavePitch", wavePitch)
        s.write("wavePosition", wavePosition)
        s.write("lastOutput", lastOutput)
        s.write("waveTable", waveTable)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("volume", volume)
        s.readSnapshotable("mod", mod)
        waveWriteEnabled = s.readBoolean("waveWriteEnabled")
        disableEnvelopes = s.readBoolean("disableEnvelopes")
        haltWaveform = s.readBoolean("haltWaveform")
        masterVolume = s.readInt("masterVolume")
        waveOverflowCounter = s.readInt("waveOverflowCounter")
        wavePitch = s.readInt("wavePitch")
        wavePosition = s.readInt("wavePosition")
        lastOutput = s.readInt("lastOutput")
        s.readIntArrayOrFill("waveTable", waveTable, 0)
    }

    companion object {

        @JvmStatic private val WAVE_VOLUME_TABLE = intArrayOf(36, 24, 17, 14)
    }
}
