package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class TxcChip(val isJv001: Boolean) :
    Memory,
    Snapshotable {

    private var accumulator: UByte = 0U
    private var inverter: UByte = 0U
    private var staging: UByte = 0U
    private var increase = false

    private val mask: UByte = if (isJv001) 0x0FU else 0x07U
    private val maskInv = mask.inv()

    var invert = isJv001
        private set

    var y = false
        private set

    var output: UByte = 0U
        private set

    private inline fun updateYFlag(value: UByte) {
        y = !invert || value.bit4
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        val value = (accumulator and mask) or ((inverter xor if (invert) 0xFFU else 0U) and maskInv)
        updateYFlag(value)
        return value
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        when {
            addr.toInt() < 0x8000 -> {
                when (addr.toInt() and 0xE103) {
                    0x4100 -> if (increase) {
                        accumulator++
                    } else {
                        accumulator = (accumulator and maskInv) or (staging and mask) xor (if (invert) 0xFFU else 0U)
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
                output = (accumulator and 0x0FU) or (inverter and 0xF0U)
            }
            else -> {
                output = (accumulator and 0x0FU) or ((inverter and 0x08U).toUInt() shl 1).toUByte()
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
        accumulator = s.readUByte("accumulator") ?: 0U
        invert = s.readBoolean("invert") ?: isJv001
        inverter = s.readUByte("inverter") ?: 0U
        staging = s.readUByte("staging") ?: 0U
        output = s.readUByte("output") ?: 0U
        increase = s.readBoolean("increase") ?: false
        y = s.readBoolean("yFlag") ?: false
    }
}