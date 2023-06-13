package br.tiagohm.nestalgia.core

data class A12RisingEdgeWatcher(private val console: Console) : Resetable, Snapshotable {

    private var a12LowClock = 0L

    override fun reset(softReset: Boolean) {
        a12LowClock = 0L
    }

    // Replace "a12Watcher.updateVRAMAddress(addr, console.ppu.frameCycle) == RISE"
    fun isRisingEdge(addr: Int): Boolean {
        if (addr and 0x1000 != 0) {
            val isRisingEdge = a12LowClock > 0 && (console.masterClock - a12LowClock) >= 3
            a12LowClock = 0L
            return isRisingEdge
        } else if (a12LowClock == 0L) {
            a12LowClock = console.masterClock
        }

        return false
    }

    override fun saveState(s: Snapshot) {
        s.write("a12LowClock", a12LowClock)
    }

    override fun restoreState(s: Snapshot) {
        a12LowClock = s.readLong("a12LowClock")
    }
}
