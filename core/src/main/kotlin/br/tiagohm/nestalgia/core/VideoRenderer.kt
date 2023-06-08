package br.tiagohm.nestalgia.core

import java.io.Closeable
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class VideoRenderer(private val console: Console) : Closeable {

    private var stop = AtomicBoolean(false)
    private var renderThread: Thread? = null
    private var rendereres = ArrayList<RenderingDevice>()
    private var waitForRender = Semaphore(1)

    override fun close() {
        stop.set(true)
        stopThread()
        rendereres.forEach { it.close() }
    }

    fun startThread() {
        if (renderThread == null) {
            stop.set(false)
            renderThread = thread(true, name = "Video Renderer", block = this::renderThread)
        }
    }

    fun stopThread() {
        stop.set(true)
        renderThread?.interrupt()
        renderThread = null
    }

    private fun renderThread() {
        rendereres.forEach { it.reset(false) }

        while (!stop.get()) {
            try {
                // Wait until a frame is ready, or until 16ms have passed (to allow UI to run at a minimum of 60fps)
                waitForRender.tryAcquire(16, TimeUnit.MILLISECONDS)
            } catch (e: Throwable) {
                break
            } finally {
                if (console.running) {
                    rendereres.forEach { it.render() }
                }
            }
        }
    }

    fun updateFrame(frame: IntArray, width: Int, height: Int) {
        if (rendereres.isNotEmpty()) {
            rendereres.forEach { it.updateFrame(frame, width, height) }
            waitForRender.release()
        }
    }

    fun registerRenderingDevice(renderer: RenderingDevice) {
        rendereres.add(renderer)

        if (rendereres.size == 1) {
            startThread()
        }
    }

    fun unregisterRenderingDevice(renderingDevice: RenderingDevice) {
        rendereres.remove(renderingDevice)

        if (rendereres.isEmpty()) {
            stopThread()
        }
    }
}
