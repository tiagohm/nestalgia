package br.tiagohm.nestalgia.desktop.audio

import br.tiagohm.nestalgia.core.AudioDevice
import br.tiagohm.nestalgia.core.Console
import br.tiagohm.nestalgia.core.SoundMixer
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

@Suppress("NOTHING_TO_INLINE", "SameParameterValue")
data class Speaker(private val console: Console) : AudioDevice {

    private var sampleRate = 0
    private var emulationSpeed = console.settings.emulationSpeed()
    private var stereo = false

    private var sourceDataLine: SourceDataLine? = null
    private var bufferSize = 0
    private var buffer: ByteArray? = null
    private var writePosition = 0
    private var readPosition = 0

    private var thread: Thread? = null
    private val pause = AtomicBoolean(false)
    private val stop = AtomicBoolean(false)

    private fun initializeAudio(sampleRate: Int, emulationSpeed: Int, stereo: Boolean) {
        stop.set(false)

        if (thread == null) {
            thread = thread(true, isDaemon = true, name = "Speaker", block = ::renderAudio)
        }

        this.emulationSpeed = emulationSpeed
        this.sampleRate = sampleRate
        this.stereo = stereo

        if (sourceDataLine == null) {
            val audioFormat = AudioFormat(
                sampleRate.toFloat(),
                16,
                2,
                true,  // PCM Signed
                false  // Little Endian
            )

            sourceDataLine = AudioSystem.getSourceDataLine(audioFormat)
            sourceDataLine!!.open(audioFormat)
            sourceDataLine!!.start()
        }

        if (buffer == null) {
            bufferSize = sourceDataLine!!.bufferSize
            buffer = ByteArray(bufferSize)
        }

        pause.set(false)

        buffer?.fill(0)

        writePosition = 0
        readPosition = 0
    }

    override fun play(buffer: ShortArray, length: Int, sampleRate: Int, stereo: Boolean) {
        updateSoundSettings()

        val bytesPerSample = SoundMixer.BITS_PER_SAMPLE / 8 * if (stereo) 2 else 1
        writeToBuffer(buffer, length * bytesPerSample)

        pause.set(false)
    }

    private inline val availableBytes
        get() = if (writePosition >= readPosition) writePosition - readPosition
        else bufferSize - readPosition + writePosition

    private fun writeToBuffer(input: ShortArray, length: Int) {
        val output = buffer ?: return

        synchronized(input) {
            when {
                writePosition + length < bufferSize -> {
                    for (i in 0 until length / 2) {
                        val s = input[i]
                        output[writePosition++] = s.toByte()
                        output[writePosition++] = (s.toInt() shr 8).toByte()
                    }
                }
                else -> {
                    val remainingBytes = bufferSize - writePosition
                    var pos = writePosition

                    for (i in 0 until remainingBytes / 2) {
                        val s = input[i]
                        output[pos++] = s.toByte()
                        output[pos++] = (s.toInt() shr 8).toByte()
                    }

                    pos = 0

                    for (i in 0 until (length - remainingBytes) / 2) {
                        val s = input[i]
                        output[pos++] = s.toByte()
                        output[pos++] = (s.toInt() shr 8).toByte()
                    }

                    writePosition = pos
                }
            }
        }
    }

    fun readBuffer(output: ByteArray, length: Int) {
        val input = buffer ?: return

        synchronized(input) {
            if (readPosition + length < bufferSize) {
                for (i in 0 until length) {
                    output[i] = input[readPosition++]
                }
            } else {
                val remainingBytes = bufferSize - readPosition

                for (i in 0 until remainingBytes) {
                    output[i] = input[readPosition + i]
                }

                for (i in 0 until length - remainingBytes) {
                    output[remainingBytes + i] = input[i]
                }

                readPosition = length - remainingBytes
            }
        }
    }

    fun renderAudio() {
        val output = ByteArray(SAMPLE_COUNT)

        while (!stop.get()) {
            if (sourceDataLine != null && availableBytes >= SAMPLE_COUNT) {
                readBuffer(output, SAMPLE_COUNT)

                if (!pause.get()) {
                    sourceDataLine!!.write(output, 0, SAMPLE_COUNT)
                }
            }

            Thread.sleep(1)
        }
    }

    override fun stop() {
        pause()

        readPosition = 0
        writePosition = 0
    }

    override fun pause() {
        pause.set(true)
    }

    override fun processEndOfFrame() = Unit

    private inline fun updateSoundSettings() {
        val sampleRate = console.settings.sampleRate
        val emulationSpeed = console.settings.emulationSpeed()

        if (sampleRate != this.sampleRate ||
            emulationSpeed != this.emulationSpeed
        ) {
            close()

            initializeAudio(sampleRate, emulationSpeed, true)
        }
    }

    override fun close() {
        stop.set(true)
        sourceDataLine?.drain()
        sourceDataLine?.stop()
        sourceDataLine?.close()
        buffer = null
        sourceDataLine = null
        thread?.interrupt()
        thread = null
    }

    companion object {

        const val SAMPLE_COUNT = 8192 * 2
    }
}
