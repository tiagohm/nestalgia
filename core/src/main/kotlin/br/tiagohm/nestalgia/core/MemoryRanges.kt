package br.tiagohm.nestalgia.core

class MemoryRanges {

    internal var ramReadAddresses = IntArray(1024)
        private set

    internal var ramWriteAddresses = IntArray(1024)
        private set

    @JvmField internal var allowOverride = false

    internal var readSize = 0
        private set

    internal var writeSize = 0
        private set

    fun addHandler(operation: MemoryAccessType, start: Int, end: Int = 0) {
        val b = if (end == 0) start else end

        if (operation.read) {
            ensureReadSize(b - start + 1)

            for (i in start..b) {
                ramReadAddresses[readSize++] = i
            }
        }

        if (operation.write) {
            ensureWriteSize(b - start + 1)

            for (i in start..b) {
                ramWriteAddresses[writeSize++] = i
            }
        }
    }

    private fun ensureReadSize(size: Int) {
        if (readSize + size > ramReadAddresses.size) {
            val data = IntArray(readSize + size)
            ramReadAddresses.copyInto(data)
            ramReadAddresses = data
        }
    }

    private fun ensureWriteSize(size: Int) {
        if (writeSize + size > ramWriteAddresses.size) {
            val data = IntArray(writeSize + size)
            ramWriteAddresses.copyInto(data)
            ramWriteAddresses = data
        }
    }
}
