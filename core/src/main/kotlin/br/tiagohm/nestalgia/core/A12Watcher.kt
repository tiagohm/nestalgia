package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.A12StateChange.*

class A12Watcher : Resetable, Snapshotable {

    @Volatile private var lastCycle = 0
    @Volatile private var cyclesDown = 0L

    override fun reset(softReset: Boolean) {
        lastCycle = 0
        cyclesDown = 0L
    }

    override fun saveState(s: Snapshot) {
        s.write("lastCycle", lastCycle)
        s.write("cycleDown", cyclesDown)
    }

    override fun restoreState(s: Snapshot) {
        lastCycle = s.readInt("lastCycle")
        cyclesDown = s.readLong("cycleDown")
    }

    fun updateVRAMAddress(addr: Int, frameCycle: Int, minDelay: Long = 10): A12StateChange {
        var result = NONE

        if (cyclesDown > 0) {
            cyclesDown += if (lastCycle > frameCycle) {
                // We changed frames
                (89342 - lastCycle) + frameCycle
            } else {
                frameCycle - lastCycle
            }
        }

        if (addr and 0x1000 == 0) {
            if (cyclesDown == 0L) {
                cyclesDown = 1L
                result = FALL
            }
        } else {
            if (cyclesDown > minDelay) {
                result = RISE
            }

            cyclesDown = 0
        }

        lastCycle = frameCycle

        return result
    }
}
