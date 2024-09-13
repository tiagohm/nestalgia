package br.tiagohm.nestalgia.core

class FdsSystemActionManager(
    console: Console,
    val mapper: Fds,
) : SystemActionManager(console) {

    @Volatile private var needEjectDisk = false
    @Volatile private var insertDiskNumber = 0
    @Volatile private var insertDiskDelay = 0

    val sideCount
        get() = mapper.sideCount

    val autoInsertDiskEnabled
        get() = mapper.autoInsertDiskEnabled

    init {
        if (console.settings.flag(EmulationFlag.FDS_AUTO_LOAD_DISK)) {
            insertDisk(0)
        }
    }

    override fun onAfterSetState() {
        super.onAfterSetState()

        if (needEjectDisk) {
            setBit(FdsButton.EJECT_DISK)
            needEjectDisk = false
        }

        if (insertDiskDelay > 0) {
            insertDiskDelay--

            if (insertDiskDelay == 0) {
                setBit(FdsButton.INSERT_DISK.bit + insertDiskNumber)
            }
        }

        val isNeedEject = isPressed(FdsButton.EJECT_DISK)
        var diskToInsert = -1

        for (i in 0..15) {
            if (isPressed(FdsButton.INSERT_DISK.bit + i)) {
                diskToInsert = i
                break
            }
        }

        synchronized(mapper) {
            if (isNeedEject || diskToInsert >= 0) {
                if (isNeedEject) {
                    mapper.ejectDisk()
                }
                if (diskToInsert >= 0) {
                    mapper.insertDisk(diskToInsert)
                }
            }
        }
    }

    fun ejectDisk() {
        needEjectDisk = true
    }

    fun insertDisk(diskNumber: Int) {
        synchronized(mapper) {
            if (mapper.diskInserted) {
                // Eject disk on next frame, then insert new disk 2 seconds later.
                needEjectDisk = true
                insertDiskNumber = diskNumber
                insertDiskDelay = REINSERT_DISK_FRAME_DELAY
            } else {
                // Insert disk on next frame.
                insertDiskNumber = diskNumber
                insertDiskDelay = 1
            }
        }

        val side = if (diskNumber % 2 == 0) "A" else "B"
        System.err.println("[FDS]: Disk ${diskNumber / 2 + 1} Side $side inserted")
    }

    fun switchDiskSide() {
        if (!autoInsertDiskEnabled) {
            synchronized(mapper) {
                if (mapper.diskInserted) {
                    console.pause()
                    insertDisk((mapper.currentDisk xor 1) % sideCount)
                    console.resume()
                }
            }
        }
    }

    fun insertNextDisk() {
        if (!autoInsertDiskEnabled) {
            synchronized(mapper) {
                console.pause()
                insertDisk(((mapper.currentDisk and 0xFE) + 2) % sideCount)
                console.resume()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("needEjectDisk", needEjectDisk)
        s.write("insertDiskNumber", insertDiskNumber)
        s.write("insertDiskDelay", insertDiskDelay)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        needEjectDisk = s.readBoolean("needEjectDisk")
        insertDiskNumber = s.readInt("insertDiskNumber")
        insertDiskDelay = s.readInt("insertDiskDelay")
    }

    companion object {

        private const val REINSERT_DISK_FRAME_DELAY = 120
    }
}
