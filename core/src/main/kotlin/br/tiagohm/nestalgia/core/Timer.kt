package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
class Timer {

    @PublishedApi
    internal var start: Long = 0

    init {
        reset()
    }

    inline fun reset() {
        start = System.currentTimeMillis()
    }

    inline val elapsedMilliseconds: Long
        get() = System.currentTimeMillis() - start

    inline fun waitUntil(targetMs: Long) {
        if (targetMs > 0) {
            val elapsedTime = elapsedMilliseconds
            val delay = targetMs - elapsedTime

            if (delay >= 1) {
                Thread.sleep(delay)
            }
        }
    }
}