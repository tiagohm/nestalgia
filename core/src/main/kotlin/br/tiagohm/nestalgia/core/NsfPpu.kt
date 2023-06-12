package br.tiagohm.nestalgia.core

class NsfPpu(console: Console) : Ppu(console) {

    override fun run(runTo: Long) {
        do {
            // Always need to run at least once, check condition
            // at the end of the loop (slightly faster).
            if (cycle < 340) {
                // Process cycles 1 to 340.
                cycle++
            } else {
                // Process cycle 0.
                processScanlineFirstCycle()
            }

            masterClock += masterClockDivider
        } while (masterClock + masterClockDivider <= runTo)
    }
}
