package br.tiagohm.nestalgia.core

// https://en.wikipedia.org/wiki/Turbo_File_(ASCII)

class AsciiTurboFile(console: Console) : ControlDevice(console, EXP_DEVICE_PORT), Battery {

    private val data = Array(SLOT_COUNT) { IntArray(FILE_SIZE) }

    private var slot = console.settings.asciiTurboFileSlot
    private var lastWrite = 0
    private var position = 0

    init {
        loadBattery()
    }

    override fun saveBattery() {
        val savedData = IntArray(FILE_SIZE * SLOT_COUNT)
        for (i in savedData.indices) data[i].copyInto(savedData, i * FILE_SIZE)
        console.batteryManager.saveBattery(".tf", savedData)
    }

    override fun loadBattery() {
        val savedData = console.batteryManager.loadBattery(".tf", FILE_SIZE * SLOT_COUNT)

        for (i in 0..3) {
            val start = i * FILE_SIZE
            val end = start + FILE_SIZE

            if (end <= savedData.size) {
                savedData.copyInto(data[i], 0, start, end)
            }
        }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr == 0x4017) {
            val i = position / 8
            val k = position % 8
            if (k == 0) slot = console.settings.asciiTurboFileSlot
            (data[slot][i] shr k and 0x01) shl 2
        } else {
            0
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        if (!value.bit1) {
            position = 0
        }

        if (!value.bit2 && lastWrite.bit2) {
            val i = position / 8
            val k = position % 8
            if (k == 0) slot = console.settings.asciiTurboFileSlot

            val s = data[slot]

            // Clock, perform write, increase position

            if (value.bit0) {
                s[i] = s[i] or (1 shl k)
            } else {
                s[i] = s[i] and (1 shl k).inv()
            }

            position = (position + 1) and (BIT_COUNT - 1)
        }

        lastWrite = value
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("position", position)
        s.write("lastWrite", lastWrite)
        s.write("slot", slot)
        s.write("data0", data[0])
        s.write("data1", data[1])
        s.write("data2", data[2])
        s.write("data3", data[3])
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        position = s.readInt("position", position)
        lastWrite = s.readInt("lastWrite", lastWrite)
        slot = s.readInt("slot", slot)
        console.settings.asciiTurboFileSlot = slot
        s.readIntArray("data0", data[0])
        s.readIntArray("data1", data[1])
        s.readIntArray("data2", data[2])
        s.readIntArray("data3", data[3])
    }

    companion object {

        const val FILE_SIZE = 0x2000
        const val SLOT_COUNT = 4
        const val BIT_COUNT = FILE_SIZE * 8
    }
}
