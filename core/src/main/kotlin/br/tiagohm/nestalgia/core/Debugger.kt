package br.tiagohm.nestalgia.core

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class Debugger(private val console: Console) : Closeable {

    private val stepCount = AtomicInteger(-1)
    private val ppuStepCount = AtomicInteger(-1)
    private val stepCycleCount = AtomicInteger(-1)
    private val stepOut = AtomicBoolean(false)
    private val breakLock = SimpleLock()
    private val stop = AtomicBoolean(false)
    private val suspendCount = AtomicBoolean(false)
    private val executionStopped = AtomicBoolean(false)
    private val breakOn = AtomicInteger(0)

    var breakOnType = BreakOnType.NONE
        private set

    val breakOnCount
        get() = breakOn.get()

    val isExecutionStopped
        get() = executionStopped.get() || console.stopped

    override fun close() {
        release()
    }

    fun release() {
        stop.set(true)
        breakLock.acquire()
        breakLock.release()
    }

    @Suppress("ControlFlowWithEmptyBody")
    fun suspend() {
        suspendCount.set(true)
        while (executionStopped.get());
    }

    fun resume() {
        suspendCount.set(false)
    }

    fun run() {
        ppuStepCount.set(-1)
        stepCount.set(-1)
        stepCycleCount.set(-1)
        stepOut.set(false)
    }

    fun processRamOperation(type: MemoryOperationType, addr: Int, value: Int) {
        if (stepCycleCount.get() > 0) {
            if (stepCycleCount.decrementAndGet() == 0) {
                step(1)
                sleepUntilResume()
            }
        }
    }

    fun processPpuCycle() {
        if (console.ppu.cycle == 0) {
            if (breakOnType == BreakOnType.SCANLINE && breakOn.get() == console.ppu.scanline) {
                step(1)
                sleepUntilResume()
            }

            if (console.ppu.scanline == 240) {
                if (breakOnType == BreakOnType.END_FRAME && breakOn.get() == console.ppu.frameCount) {
                    step(1)
                    sleepUntilResume()
                }
            }

            if (console.ppu.scanline == -1) {
                if (breakOnType == BreakOnType.START_FRAME && breakOn.get() == console.ppu.frameCount) {
                    step(1)
                    sleepUntilResume()
                }
            }
        }

        if (ppuStepCount.get() > 0) {
            if (ppuStepCount.decrementAndGet() == 0) {
                step(1)
                sleepUntilResume()
            }
        }
    }

    fun ppuStep(count: Int) {
        resetStepState()
        ppuStepCount.set(count)
    }

    fun cpuStep(count: Int) {
        resetStepState()
        stepCycleCount.set(count)
    }

    fun step(count: Int) {
        resetStepState()
        stepCount.set(count)
    }

    fun frameStep(count: Int = 1) {
        val extraScanlines = console.settings.extraScanlinesAfterNmi + console.settings.extraScanlinesBeforeNmi
        val cycleCount = ((if (console.region == Region.NTSC) 262 else 312) + extraScanlines) * 341
        ppuStep(count * cycleCount)
    }

    fun scanlineStep(count: Int = 1) {
        ppuStep(count * 341)
    }

    fun breakOn(type: BreakOnType, count: Int) {
        run()
        breakOn.set(count)
        breakOnType = type
    }

    private fun resetStepState() {
        ppuStepCount.set(-1)
        stepCycleCount.set(-1)
        stepCount.set(-1)
        stepOut.set(false)
    }

    private fun sleepUntilResume() {
        var steps = stepCount.get()

        if (steps > 0) {
            steps = stepCount.decrementAndGet()
        }

        if (steps == 0 && !stop.get() && !suspendCount.get()) {
            console.soundMixer.stopAudio()

            breakLock.acquire()
            executionStopped.set(true)
            console.notificationManager.sendNotification(NotificationType.DEBUG_BREAK)

            while (steps == 0 && !stop.get() && !suspendCount.get()) {
                Thread.sleep(100)

                if (steps == 0) {
                    console.resetRunTimers()
                }

                steps = stepCount.get()
            }

            executionStopped.set(false)
            console.notificationManager.sendNotification(NotificationType.DEBUG_CONTINUE)
            breakLock.release()
        }
    }
}
