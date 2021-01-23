package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class FdsSystemActionManager(
    console: Console,
    val mapper: Fds,
) : SystemActionManager(console) {

    private var isNeedEjectDisk = false
    private var insertDiskNumber = 0
    private var insertDiskDelay = 0

    inline val sideCount: Int
        get() = mapper.sideCount

    inline val isAutoInsertDiskEnabled: Boolean
        get() = mapper.isAutoInsertDiskEnabled

    init {
        if (console.settings.checkFlag(EmulationFlag.FDS_AUTO_LOAD_DISK)) {
            insertDisk(0)
        }
    }

    override fun onAfterSetState() {
        super.onAfterSetState()

        if (isNeedEjectDisk) {
            setBit(FdsButton.EJECT_DISK)
            isNeedEjectDisk = false
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
        isNeedEjectDisk = true
    }

    fun insertDisk(diskNumber: Int) {
        synchronized(mapper) {
            if (mapper.isDiskInserted) {
                // Eject disk on next frame, then insert new disk 2 seconds later
                isNeedEjectDisk = true
                insertDiskNumber = diskNumber
                insertDiskDelay = REINSERT_DISK_FRAME_DELAY
            } else {
                // Insert disk on next frame
                insertDiskNumber = diskNumber
                insertDiskDelay = 1
            }
        }

        val side = if (diskNumber % 2 == 0) "A" else "B"
        System.err.println("[FDS]: Disk ${diskNumber / 2 + 1} Side $side inserted")
    }

    fun switchDiskSide() {
        if (!isAutoInsertDiskEnabled) {
            synchronized(mapper) {
                if (mapper.isDiskInserted) {
                    console.pause()
                    insertDisk((mapper.currentDisk xor 1) % sideCount)
                    console.resume()
                }
            }
        }
    }

    fun insertNextDisk() {
        if (!isAutoInsertDiskEnabled) {
            synchronized(mapper) {
                console.pause()
                insertDisk(((mapper.currentDisk and 0xFE) + 2) % sideCount)
                console.resume()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("needEjectDisk", isNeedEjectDisk)
        s.write("insertDiskNumber", insertDiskNumber)
        s.write("insertDiskDelay", insertDiskDelay)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        isNeedEjectDisk = s.readBoolean("needEjectDisk") ?: false
        insertDiskNumber = s.readInt("insertDiskNumber") ?: 0
        insertDiskDelay = s.readInt("insertDiskDelay") ?: 0
    }

    companion object {
        const val REINSERT_DISK_FRAME_DELAY = 120
    }
}