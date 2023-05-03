package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
class ModulationChannel : FdsChannel() {

    private var counter = 0
    private var modulationDisabled = false

    private val modTable = UByteArray(64)
    private var modTablePosition = 0
    private var overflowCounter: UShort = 0U

    var output = 0
        private set
        get() = if (isEnabled) field else 0

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        when (addr.toInt()) {
            0x4084, 0x4086 -> super.write(addr, value, type)
            0x4085 -> updateCounter(value and 0x7FU)
            0x4087 -> {
                super.write(addr, value, type)

                modulationDisabled = value.bit7

                if (modulationDisabled) {
                    overflowCounter = 0U
                }
            }
        }
    }

    fun writeModulationTable(value: UByte) {
        if (modulationDisabled) {
            modTable[modTablePosition and 0x3F] = value and 0x07U
            modTable[(modTablePosition + 1) and 0x3F] = value and 0x07U
            modTablePosition = (modTablePosition + 2) and 0x3F
        }
    }

    private inline fun updateCounter(value: UByte) {
        counter = value.toInt()

        if (counter >= 64) {
            counter -= 128
        } else if (counter < -64) {
            counter += 128
        }
    }

    val isEnabled: Boolean
        get() = !modulationDisabled && frequency > 0U

    fun tickModulator(): Boolean {
        if (isEnabled) {
            overflowCounter = (overflowCounter + frequency).toUShort()

            if (overflowCounter < frequency) {
                // Overflowed, tick the modulator
                val offset = MOD_LUT[modTable[modTablePosition].toInt()]
                updateCounter((if (offset == RESET) 0 else counter + offset).toUByte())
                modTablePosition = (modTablePosition + 1) and 0x3F
                return true
            }
        }

        return false
    }

    fun updateOutput(volumePitch: UShort) {
        //Code from NesDev Wiki

        // pitch   = $4082/4083 (12-bit unsigned pitch value)
        // counter = $4085 (7-bit signed mod counter)
        // gain    = $4084 (6-bit unsigned mod gain)

        // 1. multiply counter by gain, lose lowest 4 bits of result but "round" in a strange way
        var temp = counter * gain.toInt()
        var remainder = temp and 0xF
        temp = temp shr 4

        if (remainder > 0 && (temp and 0x80) == 0) {
            temp += if (counter < 0) -1 else 2
        }

        // 2. wrap if a certain range is exceeded
        if (temp >= 192) {
            temp -= 256
        } else if (temp < -64) {
            temp += 256
        }

        // 3. multiply result by pitch, then round to nearest while dropping 6 bits
        temp = (volumePitch.toInt() * temp)
        remainder = temp and 0x3F
        temp = temp shr 6

        if (remainder >= 32) {
            temp += 1
        }

        output = temp
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("counter", counter)
        s.write("modulationDisabled", modulationDisabled)
        s.write("modTable", modTable)
        s.write("modTablePosition", modTablePosition)
        s.write("overflowCounter", overflowCounter)
        s.write("output", output)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        counter = s.readInt("counter") ?: 0
        modulationDisabled = s.readBoolean("modulationDisabled") ?: false
        s.readUByteArray("modTable")?.copyInto(modTable) ?: modTable.fill(0U)
        modTablePosition = s.readInt("modTablePosition") ?: 0
        overflowCounter = s.readUShort("overflowCounter") ?: 0U
        output = s.readInt("output") ?: 0
    }

    companion object {
        private const val RESET = 0xFF

        private val MOD_LUT = intArrayOf(0, 1, 2, 4, RESET, -4, -2, -1)
    }
}