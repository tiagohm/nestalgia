package br.tiagohm.nestalgia.core

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Debugger(val console: Console) : Disposable {

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

    val breakOnCount: Int
        get() = breakOn.get()

    val isExecutionStopped: Boolean
        get() = executionStopped.get() || console.isStopped

    override fun dispose() {
        release()
    }

    fun release() {
        stop.set(true)
        breakLock.acquire()
        breakLock.release()
    }

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

    fun processRamOperation(type: MemoryOperationType, addr: UShort, value: UByte) {
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

    fun breakOn(type: BreakOnType, count: Int) {
        run()
        breakOn.set(count)
        breakOnType = type
    }

    private inline fun resetStepState() {
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