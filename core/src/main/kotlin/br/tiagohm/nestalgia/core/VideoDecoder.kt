package br.tiagohm.nestalgia.core

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class VideoDecoder(private val console: Console) : AutoCloseable, Resetable {

    private val stop = AtomicBoolean()
    private val decoder = DecoderVideoFilter(console)
    @Volatile private var outputBuffer = IntArray(Ppu.PIXEL_COUNT)
    @Volatile private var decodeThread: Thread? = null
    private val waitForFrame = Semaphore(1)

    var width = Ppu.SCREEN_WIDTH
        private set

    var height = Ppu.SCREEN_HEIGHT
        private set

    val running
        get() = decodeThread != null

    override fun close() {
        stopThread()
        decoder.close()
    }

    fun stopThread() {
        if (decodeThread != null) {
            waitForFrame.release()

            decodeThread!!.interrupt()
            decodeThread = null

            console.settings.ppuModel = PpuModel.PPU_2C02

            // Clear whole screen
            outputBuffer.fill(14) // Black
            decodeFrame()
        }
    }

    override fun reset(softReset: Boolean) {
        stop.set(false)

        if (!softReset) {
            stopThread()
        }
    }

    fun startThread() {
        if (decodeThread == null) {
            stop.set(false)
            decodeThread = thread(true, name = "Video Decoder", block = this::decodeThread)
        }
    }

    private fun decodeThread() {
        while (!stop.get()) {
            try {
                waitForFrame.acquire()
            } catch (e: Throwable) {
                break
            }

            if (stop.get()) {
                return
            }

            decodeFrame()
        }
    }

    fun updateFrame(outputBuffer: IntArray) {
        this.outputBuffer = outputBuffer
        waitForFrame.release()
    }

    private fun decodeFrame() {
        val output = decoder.sendFrame(outputBuffer)
        console.videoRenderer.updateFrame(output, width, height)
    }

    fun takeScreenshot(): IntArray {
        return decoder.takeScreenshot()
    }
}
