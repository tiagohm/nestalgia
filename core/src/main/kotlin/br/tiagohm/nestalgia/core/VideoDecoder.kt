package br.tiagohm.nestalgia.core

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@Suppress("NOTHING_TO_INLINE")
class VideoDecoder(val console: Console) :
    Disposable,
    Resetable {

    private var stop = AtomicBoolean()
    private val videoFilter = DefaultVideoFilter(console)
    private var outputBuffer = UShortArray(Ppu.PIXEL_COUNT)
    private var frameNumber = 0
    private var decodeThread: Thread? = null
    private val waitForFrame = Semaphore(1)

    var width: Int = Ppu.SCREEN_WIDTH
        private set

    var height: Int = Ppu.SCREEN_HEIGHT
        private set

    val isRunning: Boolean
        get() = decodeThread != null

    override fun dispose() {
        stopThread()
        videoFilter.dispose()
    }

    fun stopThread() {
        if (decodeThread != null) {
            waitForFrame.release()

            decodeThread!!.interrupt()
            decodeThread = null

            console.settings.ppuModel = PpuModel.PPU_2C02

            // Clear whole screen
            outputBuffer.fill(14U) // Black
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
            } catch (e: Exception) {
                break
            }

            if (stop.get()) {
                return
            }

            decodeFrame()
        }
    }

    fun updateFrame(outputBuffer: UShortArray) {
        frameNumber = console.frameCount
        this.outputBuffer = outputBuffer
        waitForFrame.release()
    }

    private inline fun decodeFrame() {
        val output = videoFilter.sendFrame(outputBuffer, frameNumber)
        console.videoRenderer.updateFrame(output, width, height)
    }

    fun takeScreenshot(): IntArray {
        return videoFilter.takeScreenshot()
    }
}
