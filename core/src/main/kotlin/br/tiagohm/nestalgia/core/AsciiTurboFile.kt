package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*

// https://en.wikipedia.org/wiki/Turbo_File_(ASCII)

class AsciiTurboFile(console: Console) : ControlDevice(console, ASCII_TURBO_FILE, EXP_DEVICE_PORT), Battery {

    private val data = IntArray(FILE_SIZE)

    private var lastWrite = 0
    private var position = 0

    init {
        loadBattery()
    }

    override fun saveBattery() {
        console.batteryManager.saveBattery(".tf", data)
    }

    override fun loadBattery() {
        val savedData = console.batteryManager.loadBattery(".tf", FILE_SIZE)
        savedData.copyInto(data, 0)
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr == 0x4017) {
            val i = position / 8
            val k = position % 8
            (data[i] shr k and 0x01) shl 2
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

            // Clock, perform write, increase position.

            if (value.bit0) {
                data[i] = data[i] or (1 shl k)
            } else {
                data[i] = data[i] and (1 shl k).inv()
            }

            position = (position + 1) and (BIT_COUNT - 1)
        }

        lastWrite = value
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("position", position)
        s.write("lastWrite", lastWrite)
        s.write("data", data)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        position = s.readInt("position", position)
        lastWrite = s.readInt("lastWrite", lastWrite)
        s.readIntArray("data", data)
    }

    companion object {

        const val FILE_SIZE = 0x2000
        const val BIT_COUNT = FILE_SIZE * 8
    }
}
