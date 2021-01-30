package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class BattleBox(console: Console) :
    ControlDevice(console, EXP_DEVICE_PORT),
    Battery {

    private var lastWrite: UByte = 0U
    private var address = 0
    private var chipSelect = false
    private val data = UShortArray(FILE_SIZE / 2)
    private var output: UByte = 0U
    private var writeEnabled = false
    private var inputBitPosition = 0
    private var inputData = 0
    private var isWrite = false
    private var isRead = false

    init {
        loadBattery()
    }

    override fun loadBattery() {
        val data = UByteArray(FILE_SIZE)
        console.batteryManager.loadBattery(".bb").copyInto(data)

        for (i in data.indices step 2) {
            this.data[i / 2] = makeUShort(data[i], data[i + 1]) // Little Endian
        }
    }

    override fun saveBattery() {
        val data = UByteArray(FILE_SIZE)

        for (i in this.data.indices) {
            data[i * 2] = this.data[i].loByte
            data[i * 2 + 1] = this.data[i].hiByte
        }

        console.batteryManager.saveBattery(".bb", data)
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        return if (addr.toInt() == 0x4017) {
            if (lastWrite.bit0) {
                chipSelect = !chipSelect
                inputData = 0
                inputBitPosition = 0
            }

            output = output xor 0x01U

            val readBit = if (isRead) {
                val index = (if (chipSelect) 0x80 else 0) or address
                ((data[index] shr inputBitPosition) and 0x01U).toInt() shl 3
            } else {
                0
            }

            val writeBit = output.toInt() shl 4

            (readBit or writeBit).toUByte()
        } else {
            0U
        }
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        if (value.bit0 && !lastWrite.bit0) {
            // Clock
            inputData = inputData and (1 shl inputBitPosition).inv()
            inputData = inputData or (output.toInt() shl inputBitPosition)
            inputBitPosition++

            if (inputBitPosition > 15) {
                if (isWrite) {
                    data[(if (chipSelect) 0x80 else 0) or address] = inputData.toUShort()
                    isWrite = false
                } else {
                    isRead = false

                    // Done reading addr/command or write data
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
                        // Chip erase
                        0x0C ->
                            if (writeEnabled) {
                                data.fill(0U)
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

        lastWrite = s.readUByte("lastWrite") ?: 0U
        address = s.readInt("address") ?: 0
        chipSelect = s.readBoolean("chipSelect") ?: false
        s.readUShortArray("data")?.copyInto(data) ?: data.fill(0U)
        output = s.readUByte("output") ?: 0U
        writeEnabled = s.readBoolean("writeEnabled") ?: false
        inputBitPosition = s.readInt("inputBitPosition") ?: 0
        inputData = s.readInt("inputData") ?: 0
        isWrite = s.readBoolean("isWrite") ?: false
        isRead = s.readBoolean("isRead") ?: false
    }

    companion object {
        const val FILE_SIZE = 0x200
    }
}