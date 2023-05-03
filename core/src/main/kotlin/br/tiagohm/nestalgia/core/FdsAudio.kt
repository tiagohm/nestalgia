package br.tiagohm.nestalgia.core

import kotlin.math.min

// https://wiki.nesdev.com/w/index.php/FDS_audio

@Suppress("NOTHING_TO_INLINE")
class FdsAudio(console: Console) :
    ExpansionAudio(console),
    Memory {

    private val waveTable = UByteArray(64)
    private var waveWriteEnabled = false

    private val volume = FdsChannel()
    private val mod = ModulationChannel()

    private var disableEnvelopes = false
    private var haltWaveform = false

    private var masterVolume: UByte = 0U

    // Internal values
    private var waveOverflowCounter = 0
    private var wavePitch = 0
    private var wavePosition: UByte = 0U

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
            wavePosition = 0U
            updateOutput()
        } else {
            updateOutput()

            if ((frequency.toInt() + mod.output) > 0 && !waveWriteEnabled) {
                waveOverflowCounter += frequency.toInt() + mod.output

                if (waveOverflowCounter < frequency.toInt() + mod.output) {
                    wavePosition = ((wavePosition + 1U) and 0x3FU).toUByte()
                }
            }
        }
    }

    private inline fun updateOutput() {
        val level = min(volume.gain.toUInt(), 32U) * WAVE_VOLUME_TABLE[masterVolume.toInt()]
        val outputLevel = ((waveTable[wavePosition.toInt()] * level) / 1152U).toInt() and 0xFF

        if (lastOutput != outputLevel) {
            console.apu.addExpansionAudioDelta(AudioChannel.FDS, outputLevel - lastOutput)
            lastOutput = outputLevel
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        var value = console.memoryManager.getOpenBus()

        when {
            addr <= 0x407FU -> {
                value = value and 0xC0U
                value = value or waveTable[addr.toInt() and 0x3F]
            }
            addr.toInt() == 0x4090 -> {
                value = value and 0xC0U
                value = value or volume.gain
            }
            addr.toInt() == 0x4092 -> {
                value = value and 0xC0U
                value = value or mod.gain
            }
        }

        return value
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        if (addr <= 0x407FU) {
            if (waveWriteEnabled) {
                waveTable[addr.toInt() and 0x3F] = value and 0x3FU
            }
        } else {
            when (addr.toInt()) {
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
                0x4084, 0x4085, 0x4086, 0x4087 -> mod.write(addr, value, type)
                0x4088 -> mod.writeModulationTable(value)
                0x4089 -> {
                    masterVolume = value and 0x03U
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

        s.readSnapshot("volume")?.let { volume.restoreState(it) }
        s.readSnapshot("mod")?.let { mod.restoreState(it) }
        waveWriteEnabled = s.readBoolean("waveWriteEnabled") ?: false
        disableEnvelopes = s.readBoolean("disableEnvelopes") ?: false
        haltWaveform = s.readBoolean("haltWaveform") ?: false
        masterVolume = s.readUByte("masterVolume") ?: 0U
        waveOverflowCounter = s.readInt("waveOverflowCounter") ?: 0
        wavePitch = s.readInt("wavePitch") ?: 0
        wavePosition = s.readUByte("wavePosition") ?: 0U
        lastOutput = s.readInt("lastOutput") ?: 0
        s.readUByteArray("waveTable")?.copyInto(waveTable) ?: waveTable.fill(0U)
    }

    companion object {
        private val WAVE_VOLUME_TABLE = uintArrayOf(36U, 24U, 17U, 14U)
    }
}