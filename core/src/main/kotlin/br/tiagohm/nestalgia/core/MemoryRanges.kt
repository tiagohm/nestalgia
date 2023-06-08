package br.tiagohm.nestalgia.core

class MemoryRanges {

    @JvmField @Volatile internal var ramReadAddresses = IntArray(1024)
    @JvmField @Volatile internal var ramWriteAddresses = IntArray(1024)
    @JvmField @Volatile internal var allowOverride = false
    @JvmField @Volatile internal var readSize = 0
    @JvmField @Volatile internal var writeSize = 0

    fun addHandler(operation: MemoryOperation, start: Int, end: Int = 0) {
        val b = if (end == 0) start else end

        if (operation.read) {
            for (i in start..b) {
                ensureReadSize(b - start + 1)
                ramReadAddresses[readSize++] = i
            }
        }

        if (operation.write) {
            for (i in start..b) {
                ensureWriteSize(b - start + 1)
                ramWriteAddresses[writeSize++] = i
            }
        }
    }

    private fun ensureReadSize(size: Int) {
        if (readSize + size >= ramReadAddresses.size) {
            val data = IntArray(ramReadAddresses.size * 2)
            ramReadAddresses.copyInto(data)
            ramReadAddresses = data
        }
    }

    private fun ensureWriteSize(size: Int) {
        if (writeSize + size >= ramWriteAddresses.size) {
            val data = IntArray(ramWriteAddresses.size * 2)
            ramWriteAddresses.copyInto(data)
            ramWriteAddresses = data
        }
    }
}
