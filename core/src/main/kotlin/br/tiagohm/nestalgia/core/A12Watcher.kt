package br.tiagohm.nestalgia.core

class A12Watcher :
    Resetable,
    Snapshotable {

    private var lastCycle = 0
    private var cyclesDown = 0L

    override fun reset(softReset: Boolean) {
        lastCycle = 0
        cyclesDown = 0
    }

    override fun saveState(s: Snapshot) {
        s.write("lastCycle", lastCycle)
        s.write("cycleDown", cyclesDown)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        lastCycle = s.readInt("lastCycle") ?: 0
        cyclesDown = s.readLong("cycleDown") ?: 0
    }

    fun updateVRAMAddress(addr: UShort, frameCycle: Int, minDelay: Long = 10): A12StateChange {
        var result = A12StateChange.NONE

        if (cyclesDown > 0) {
            cyclesDown += if (lastCycle > frameCycle) {
                // We changed frames
                (89342 - lastCycle) + frameCycle
            } else {
                frameCycle - lastCycle
            }
        }

        if (addr.toInt() and 0x1000 == 0) {
            if (cyclesDown == 0L) {
                cyclesDown = 1
                result = A12StateChange.FALL
            }
        } else {
            if (cyclesDown > minDelay) {
                result = A12StateChange.RISE
            }

            cyclesDown = 0
        }

        lastCycle = frameCycle

        return result
    }
}