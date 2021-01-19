package br.tiagohm.nestalgia.core

import java.util.*

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class SoundMixer(val console: Console) :
    Resetable,
    Disposable,
    Snapshotable {

    private var clockRate = 0
    private val outputBuffer = ShortArray(MAX_SAMPLES_PER_FRAME)
    private val blip = Blip(MAX_SAMPLES_PER_FRAME)
    private var sampleRate = console.settings.sampleRate
    private val devices = ArrayList<AudioDevice>(1)
    private var previousOutputLeft: UShort = 0U
    private var previousOutputRight: UShort = 0U
    private val channelOutput = Array(MAX_CHANNEL_COUNT) { ShortArray(10000) }
    private val currentOutput = ShortArray(MAX_CHANNEL_COUNT)
    private var previousTargetRate = 0.0
    private val timestamps = TreeSet<Int>()
    private var muteFrameCount = 0U

    private var privateRegion = Region.NTSC
    var region: Region
        get() = privateRegion
        set(value) {
            privateRegion = value
            updateRates(true)
        }

    fun registerAudioDevice(device: AudioDevice) {
        devices.add(device)
    }

    fun unregisterAudioDevice(device: AudioDevice) {
        devices.remove(device)
    }

    override fun reset(softReset: Boolean) {
        muteFrameCount = 0U

        previousOutputLeft = 0U
        previousOutputRight = 0U

        blip.clear()

        channelOutput.forEach { it.fill(0) }
        currentOutput.fill(0)

        updateRates(true)

        previousTargetRate = sampleRate.toDouble()
    }

    override fun dispose() {
        devices.forEach { it.dispose() }
    }

    fun stopAudio(clearBuffer: Boolean = false) {
        devices.forEach { if (clearBuffer) it.stop() else it.pause() }
    }

    fun processEndOfFrame() {
        devices.forEach { it.processEndOfFrame() }
    }

    fun getChannelOutput(channel: AudioChannel): Double {
        return currentOutput[channel.ordinal] * 2.0
    }

    val outputVolume: UShort
        get() {
            val squareOutput = getChannelOutput(AudioChannel.SQUARE_1) + getChannelOutput(AudioChannel.SQUARE_2)
            val tndOutput = (3 * getChannelOutput(AudioChannel.TRIANGLE) +
                    2 * getChannelOutput(AudioChannel.NOISE) +
                    getChannelOutput(AudioChannel.DMC))

            val squareVolume = 477600 / (8128.0 / squareOutput + 100.0)
            val tndVolume = 818350 / (24329.0 / tndOutput + 100.0)

            return (squareVolume +
                    tndVolume +
                    getChannelOutput(AudioChannel.FDS) * 20 +
                    getChannelOutput(AudioChannel.MMC5) * 43 +
                    getChannelOutput(AudioChannel.NAMCO_163) * 20 +
                    getChannelOutput(AudioChannel.SUNSOFT_5B) * 15 +
                    getChannelOutput(AudioChannel.VRC6) * 75 +
                    getChannelOutput(AudioChannel.VRC7)).toInt().toUShort()
        }

    fun addDelta(channel: AudioChannel, time: Int, delta: Short) {
        if (delta.toInt() != 0) {
            timestamps.add(time)
            channelOutput[channel.ordinal][time] =
                (channelOutput[channel.ordinal][time] + delta).toShort()
        }
    }

    private inline fun endFrame(time: Int) {
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
            blip.addDelta(stamp, ((currentOutput - previousOutputLeft).toInt() * 4.0).toInt())
            previousOutputLeft = currentOutput
        }

        blip.endFrame(time)

        if (muteFrame) {
            muteFrameCount++
        } else {
            muteFrameCount = 0U
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

        if (devices.isNotEmpty() && !console.isPaused) {
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

    private inline fun updateRates(force: Boolean) {
        var newRate = region.clockRate

        if (console.settings.checkFlag(EmulationFlag.INTEGER_FPS_MODE)) {
            // Adjust sample rate when running at 60.0 fps instead of 60.1
            newRate = if (region == Region.NTSC) {
                (newRate * 60.0 / 60.0988118623484).toInt()
            } else {
                (newRate * 50.0 / 50.00697796826829).toInt()
            }
        }

        val targetRate = sampleRate * getTargetRateAdjustment()

        if (clockRate != newRate || force) {
            clockRate = newRate
            blip.setRates(clockRate.toDouble(), targetRate)
        }
    }

    private inline fun getTargetRateAdjustment(): Double {
        return 100.0 / console.settings.getEmulationSpeed()
    }

    private inline fun updateTargetSampleRate() {
        val targetRate = sampleRate * getTargetRateAdjustment()

        if (targetRate != previousTargetRate) {
            previousTargetRate = targetRate
            blip.setRates(clockRate.toDouble(), targetRate)
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("clockRate", clockRate)
        s.write("sampleRate", sampleRate)
        s.write("region", privateRegion)
        s.write("previousOutputLeft", previousOutputLeft)
        s.write("currentOutput", currentOutput)
        s.write("previousOutputRight", previousOutputRight)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        clockRate = s.readInt("clockRate") ?: 0
        sampleRate = s.readInt("sampleRate") ?: console.settings.sampleRate
        privateRegion = s.readEnum("region") ?: Region.NTSC

        reset(true)

        previousOutputLeft = s.readUShort("previousOutputLeft") ?: 0U
        s.readShortArray("currentOutput")?.copyInto(currentOutput)
        previousOutputRight = s.readUShort("previousOutputRight") ?: 0U
    }

    companion object {
        const val CYCLE_LENGTH = 10000
        const val BITS_PER_SAMPLE = 16

        const val MAX_SAMPLE_RATE = 96000

        // x4 to allow CPU overclocking up to 10x, x2 for panning stereo
        const val MAX_SAMPLES_PER_FRAME = MAX_SAMPLE_RATE / 60 * 4 * 2
        const val MAX_CHANNEL_COUNT = 11
    }
}
