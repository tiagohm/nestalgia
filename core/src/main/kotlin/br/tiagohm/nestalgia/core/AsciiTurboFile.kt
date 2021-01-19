package br.tiagohm.nestalgia.core

// https://en.wikipedia.org/wiki/Turbo_File_(ASCII)

@ExperimentalUnsignedTypes
class AsciiTurboFile(console: Console) :
    ControlDevice(console, EXP_DEVICE_PORT),
    Battery {

    private val data = Array(SLOT_COUNT) { UByteArray(FILE_SIZE) }
    private var lastWrite: UByte = 0U
    private var position = 0
    private var slot = console.settings.asciiTurboFileSlot

    init {
        loadBattery()
    }

    override fun saveBattery() {
        val data = UByteArray(FILE_SIZE * SLOT_COUNT)
        for (i in 0..3) this.data[i].copyInto(data, i * FILE_SIZE)
        console.batteryManager.saveBattery(".tf", data)
    }

    override fun loadBattery() {
        val data = UByteArray(FILE_SIZE * SLOT_COUNT)
        console.batteryManager.loadBattery(".tf", data.size).copyInto(data)

        for (i in 0..3) {
            val start = i * FILE_SIZE
            val end = start + FILE_SIZE

            if (end <= data.size) {
                data.copyInto(this.data[i], 0, start, end)
            }
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        return if (addr.toInt() == 0x4017) {
            val i = position / 8
            val k = position % 8
            if (k == 0) slot = console.settings.asciiTurboFileSlot
            (((data[slot][i] shr k) and 0x01U).toInt() shl 2).toUByte()
        } else {
            0U
        }
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
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
                s[i] = s[i] or (1 shl k).toUByte()
            } else {
                s[i] = s[i] and (1 shl k).inv().toUByte()
            }

            position = (position + 1) and (BIT_COUNT - 1)
        }

        lastWrite = value
    }

    companion object {
        const val FILE_SIZE = 0x2000
        const val SLOT_COUNT = 4
        const val BIT_COUNT = FILE_SIZE * 8
    }
}