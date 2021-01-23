package br.tiagohm.nestalgia.core

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Suppress("NOTHING_TO_INLINE")
class SimpleLock {
    private val lock = AtomicBoolean(false)
    private val lockCount = AtomicInteger(0)
    private var holderId = Thread.currentThread().id

    @Suppress("ControlFlowWithEmptyBody")
    fun acquire() {
        if (lockCount.get() == 0 || holderId != Thread.currentThread().id) {
            while (lock.getAndSet(true));
            holderId = Thread.currentThread().id
            lockCount.set(1)
        } else {
            // Same thread can acquire the same lock multiple times
            lockCount.incrementAndGet()
        }
    }

    val isFree: Boolean
        get() = lockCount.get() == 0

    inline fun waitForRelease() {
        acquire()
        release()
    }

    fun release() {
        if (lockCount.get() > 0 && holderId == Thread.currentThread().id) {
            if (lockCount.decrementAndGet() == 0) {
                holderId = Thread.currentThread().id
                lock.set(false)
            }
        }
    }
}