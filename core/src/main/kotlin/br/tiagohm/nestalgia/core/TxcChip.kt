package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
class TxcChip(val isJv001: Boolean) : Memory, Resetable, Snapshotable {

    private var accumulator = 0
    private var inverter = 0
    private var staging = 0
    private var increase = false

    private val mask = if (isJv001) 0x0F else 0x07
    private val maskInv = mask.inv() and 0xFF

    var invert = isJv001
        private set

    var y = false
        private set

    var output = 0
        private set

    override fun reset(softReset: Boolean) {
        accumulator = 0
        inverter = 0
        staging = 0
        increase = false
        invert = isJv001
        output = 0
        y = false
    }

    private inline fun updateYFlag(value: Int) {
        y = !invert || value.bit4
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        val value = (accumulator and mask) or ((inverter xor if (invert) 0xFF else 0) and maskInv)
        updateYFlag(value)
        return value
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        when {
            addr < 0x8000 -> {
                when (addr and 0xE103) {
                    0x4100 -> if (increase) {
                        accumulator++
                    } else {
                        accumulator = (accumulator and maskInv) or (staging and mask) xor (if (invert) 0xFF else 0)
                    }
                    0x4101 -> invert = value.bit0
                    0x4102 -> {
                        staging = value and mask
                        inverter = value and maskInv
                    }
                    0x4103 -> increase = value.bit0
                }
            }
            isJv001 -> {
                output = (accumulator and 0x0F) or (inverter and 0xF0)
            }
            else -> {
                output = (accumulator and 0x0F) or (inverter and 0x08 shl 1)
            }
        }

        updateYFlag(value)
    }

    override fun saveState(s: Snapshot) {
        s.write("accumulator", accumulator)
        s.write("invert", invert)
        s.write("inverter", inverter)
        s.write("staging", staging)
        s.write("output", output)
        s.write("increase", increase)
        s.write("yFlag", y)
    }

    override fun restoreState(s: Snapshot) {
        accumulator = s.readInt("accumulator")
        invert = s.readBoolean("invert", isJv001)
        inverter = s.readInt("inverter")
        staging = s.readInt("staging")
        output = s.readInt("output")
        increase = s.readBoolean("increase")
        y = s.readBoolean("yFlag")
    }
}
