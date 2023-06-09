package br.tiagohm.nestalgia.core

class BattleBox(console: Console) : ControlDevice(console, EXP_DEVICE_PORT), Battery {

    private var lastWrite = 0
    private var address = 0
    private var chipSelect = false
    private val data = IntArray(FILE_SIZE / 2)
    private var output = 0
    private var writeEnabled = false
    private var inputBitPosition = 0
    private var inputData = 0
    private var isWrite = false
    private var isRead = false

    init {
        loadBattery()
    }

    override fun loadBattery() {
        val savedData = console.batteryManager.loadBattery(".bb", FILE_SIZE)

        for (i in savedData.indices step 2) {
            data[i / 2] = savedData[i] or (savedData[i + 1] shl 8) // Little Endian
        }
    }

    override fun saveBattery() {
        val savedData = IntArray(FILE_SIZE)

        for (i in data.indices) {
            savedData[i * 2] = data[i].loByte
            savedData[i * 2 + 1] = data[i].hiByte
        }

        console.batteryManager.saveBattery(".bb", savedData)
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr == 0x4017) {
            if (lastWrite.bit0) {
                chipSelect = !chipSelect
                inputData = 0
                inputBitPosition = 0
            }

            output = output xor 0x01

            val readBit = if (isRead) {
                val index = (if (chipSelect) 0x80 else 0) or address
                data[index] shr inputBitPosition and 0x01 shl 3
            } else {
                0
            }

            val writeBit = output shl 4

            readBit or writeBit
        } else {
            0
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        if (value.bit0 && !lastWrite.bit0) {
            // Clock.
            inputData = inputData and (1 shl inputBitPosition).inv()
            inputData = inputData or (output shl inputBitPosition)

            inputBitPosition++

            if (inputBitPosition > 15) {
                if (isWrite) {
                    data[(if (chipSelect) 0x80 else 0) or address] = inputData
                    isWrite = false
                } else {
                    isRead = false

                    // Done reading addr/command or write data.
                    val address = inputData and 0x7F

                    when (((inputData and 0x7F00) shr 8) xor 0x7F) {
                        // Read
                        0x01 -> {
                            this.address = address
                            isRead = true
                        }
                        // Program
                        0x06 ->
                            if (writeEnabled) {
                                this.address = address
                                isWrite = true
                            }
                        // Chip erase.
                        0x0C ->
                            if (writeEnabled) {
                                data.fill(0)
                            }
                        0x0D -> {
                        }
                        0x09 -> writeEnabled = true
                        0x0B -> writeEnabled = false
                    }
                }

                inputBitPosition = 0
            }
        }

        lastWrite = value
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("lastWrite", lastWrite)
        s.write("address", address)
        s.write("chipSelect", chipSelect)
        s.write("data", data)
        s.write("output", output)
        s.write("writeEnabled", writeEnabled)
        s.write("inputBitPosition", inputBitPosition)
        s.write("inputData", inputData)
        s.write("isWrite", isWrite)
        s.write("isRead", isRead)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        lastWrite = s.readInt("lastWrite")
        address = s.readInt("address")
        chipSelect = s.readBoolean("chipSelect")
        s.readIntArrayOrFill("data", data, 0)
        output = s.readInt("output")
        writeEnabled = s.readBoolean("writeEnabled")
        inputBitPosition = s.readInt("inputBitPosition")
        inputData = s.readInt("inputData")
        isWrite = s.readBoolean("isWrite")
        isRead = s.readBoolean("isRead")
    }

    companion object {

        const val FILE_SIZE = 0x200
    }
}
