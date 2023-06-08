package br.tiagohm.nestalgia.core

import java.io.Closeable
import java.util.*

class SoundMixer(private val console: Console) : Resetable, Closeable, Snapshotable {

    private var clockRate = 0
    private val outputBuffer = ShortArray(MAX_SAMPLES_PER_FRAME)
    private val blip = Blip(MAX_SAMPLES_PER_FRAME)
    private var sampleRate = console.settings.sampleRate
    private val devices = ArrayList<AudioDevice>(1)
    private var previousOutputLeft = 0
    private var previousOutputRight = 0
    private val channelOutput = Array(MAX_CHANNEL_COUNT) { ShortArray(10000) }
    private val currentOutput = ShortArray(MAX_CHANNEL_COUNT)
    private var previousTargetRate = 0.0
    private val timestamps = TreeSet<Int>()
    private var muteFrameCount = 0
    private var mRegion = Region.NTSC

    var region: Region
        get() = mRegion
        set(value) {
            mRegion = value
            updateRates(true)
        }

    fun registerAudioDevice(device: AudioDevice) {
        devices.add(device)
    }

    fun unregisterAudioDevice(device: AudioDevice) {
        devices.remove(device)
    }

    override fun reset(softReset: Boolean) {
        muteFrameCount = 0

        previousOutputLeft = 0
        previousOutputRight = 0

        blip.clear()

        channelOutput.forEach { it.fill(0) }
        currentOutput.fill(0)

        updateRates(true)

        previousTargetRate = sampleRate.toDouble()
    }

    override fun close() {
        devices.forEach { it.close() }
    }

    fun stopAudio(clearBuffer: Boolean = false) {
        devices.forEach { if (clearBuffer) it.stop() else it.pause() }
    }

    fun processEndOfFrame() {
        devices.forEach { it.processEndOfFrame() }
    }

    fun channelOutput(channel: AudioChannel): Double {
        return currentOutput[channel.ordinal] * 2.0
    }

    val outputVolume: Int
        get() {
            val squareOutput = channelOutput(AudioChannel.SQUARE_1) + channelOutput(AudioChannel.SQUARE_2)
            val tndOutput = (3 * channelOutput(AudioChannel.TRIANGLE) +
                2 * channelOutput(AudioChannel.NOISE) +
                channelOutput(AudioChannel.DMC))

            val squareVolume = 477600 / (8128.0 / squareOutput + 100.0)
            val tndVolume = 818350 / (24329.0 / tndOutput + 100.0)

            return (squareVolume +
                tndVolume +
                channelOutput(AudioChannel.FDS) * 20 +
                channelOutput(AudioChannel.MMC5) * 43 +
                channelOutput(AudioChannel.NAMCO_163) * 20 +
                channelOutput(AudioChannel.SUNSOFT_5B) * 15 +
                channelOutput(AudioChannel.VRC6) * 75 +
                channelOutput(AudioChannel.VRC7)).toInt()
        }

    fun addDelta(channel: AudioChannel, time: Int, delta: Int) {
        if (delta != 0) {
            timestamps.add(time)
            channelOutput[channel.ordinal][time] = (channelOutput[channel.ordinal][time] + delta).toShort()
        }
    }

    private fun endFrame(time: Int) {
        var muteFrame = true

        for (stamp in timestamps) {
            for (j in 0 until MAX_CHANNEL_COUNT) {
                if (channelOutput[j][stamp].toInt() != 0) {
                    // Assume any change in output means sound is playing, disregarding volume options
                    // NSF tracks that mute the triangle channel by setting it to a high-frequency value will not be considered silent
                    muteFrame = false
                }

                currentOutput[j] = (currentOutput[j] + channelOutput[j][stamp]).toShort()
            }

            val currentOutput = outputVolume
            blip.addDelta(stamp, (currentOutput - previousOutputLeft) * 4)
            previousOutputLeft = currentOutput
        }

        blip.endFrame(time)

        if (muteFrame) {
            muteFrameCount++
        } else {
            muteFrameCount = 0
        }

        if (timestamps.isNotEmpty()) {
            timestamps.clear()
            channelOutput.forEach { it.fill(0) }
        }
    }

    fun playAudioBuffer(time: Int) {
        updateTargetSampleRate()
        endFrame(time)

        val sampleCount = blip.readSample(outputBuffer, MAX_SAMPLES_PER_FRAME, true)

        //Copy left channel to right channel (optimization - when no panning is used)
        var i = 0

        while (i < sampleCount * 2) {
            outputBuffer[i + 1] = outputBuffer[i]
            i += 2
        }

        console.mapper!!.applySamples(outputBuffer, sampleCount, 4.0)

        if (devices.isNotEmpty() && !console.paused) {
            devices.forEach { it.play(outputBuffer, sampleCount, sampleRate, true) }
        }

        if (console.settings.needAudioSettingsUpdate()) {
            if (console.settings.sampleRate != sampleRate) {
                sampleRate = console.settings.sampleRate
                updateRates(true)
            } else {
                updateRates(false)
            }
        }
    }

    private fun updateRates(force: Boolean) {
        var newRate = region.clockRate

        if (console.settings.flag(EmulationFlag.INTEGER_FPS_MODE)) {
            // Adjust sample rate when running at 60.0 fps instead of 60.1
            newRate = if (region == Region.NTSC) {
                (newRate * 60.0 / 60.0988118623484).toInt()
            } else {
                (newRate * 50.0 / 50.00697796826829).toInt()
            }
        }

        val targetRate = sampleRate * targetRateAdjustment

        if (clockRate != newRate || force) {
            clockRate = newRate
            blip.rates(clockRate.toDouble(), targetRate)
        }
    }

    private val targetRateAdjustment
        get() = 100.0 / console.settings.emulationSpeed()

    private fun updateTargetSampleRate() {
        val targetRate = sampleRate * targetRateAdjustment

        if (targetRate != previousTargetRate) {
            previousTargetRate = targetRate
            blip.rates(clockRate.toDouble(), targetRate)
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("clockRate", clockRate)
        s.write("sampleRate", sampleRate)
        s.write("region", mRegion)
        s.write("previousOutputLeft", previousOutputLeft)
        s.write("currentOutput", currentOutput)
        s.write("previousOutputRight", previousOutputRight)
    }

    override fun restoreState(s: Snapshot) {
        clockRate = s.readInt("clockRate")
        sampleRate = s.readInt("sampleRate", console.settings.sampleRate)
        mRegion = s.readEnum("region", Region.NTSC)

        reset(true)

        previousOutputLeft = s.readInt("previousOutputLeft")
        s.readShortArray("currentOutput")?.copyInto(currentOutput)
        previousOutputRight = s.readInt("previousOutputRight")
    }

    companion object {

        const val CYCLE_LENGTH = 10000
        const val BITS_PER_SAMPLE = 16

        const val MAX_SAMPLE_RATE = 96000

        // x4 to allow CPU overclocking up to 10x, x2 for panning stereo.
        const val MAX_SAMPLES_PER_FRAME = MAX_SAMPLE_RATE / 60 * 4 * 2
        const val MAX_CHANNEL_COUNT = 11
    }
}
