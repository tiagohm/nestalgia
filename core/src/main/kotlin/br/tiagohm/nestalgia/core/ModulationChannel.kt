package br.tiagohm.nestalgia.core

class ModulationChannel : FdsChannel() {

    private var counter = 0
    private var modulationDisabled = false

    private val modTable = IntArray(64)
    private var modTablePosition = 0
    private var overflowCounter = 0

    var output = 0
        private set
        get() = if (isEnabled) field else 0

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        when (addr) {
            0x4084, 0x4086 -> super.write(addr, value, type)
            0x4085 -> updateCounter(value and 0x7F)
            0x4087 -> {
                super.write(addr, value, type)

                modulationDisabled = value.bit7

                if (modulationDisabled) {
                    overflowCounter = 0
                }
            }
        }
    }

    fun writeModulationTable(value: Int) {
        if (modulationDisabled) {
            modTable[modTablePosition and 0x3F] = value and 0x07
            modTable[(modTablePosition + 1) and 0x3F] = value and 0x07
            modTablePosition = (modTablePosition + 2) and 0x3F
        }
    }

    private fun updateCounter(value: Int) {
        counter = value

        if (counter >= 64) {
            counter -= 128
        } else if (counter < -64) {
            counter += 128
        }
    }

    val isEnabled: Boolean
        get() = !modulationDisabled && frequency > 0

    fun tickModulator(): Boolean {
        if (isEnabled) {
            overflowCounter += frequency

            if (overflowCounter < frequency) {
                // Overflowed, tick the modulator
                val offset = MOD_LUT[modTable[modTablePosition]]
                updateCounter(if (offset == RESET) 0 else counter + offset)
                modTablePosition = (modTablePosition + 1) and 0x3F
                return true
            }
        }

        return false
    }

    fun updateOutput(volumePitch: Int) {
        //Code from NesDev Wiki

        // pitch   = $4082/4083 (12-bit unsigned pitch value)
        // counter = $4085 (7-bit signed mod counter)
        // gain    = $4084 (6-bit unsigned mod gain)

        // 1. multiply counter by gain, lose lowest 4 bits of result but "round" in a strange way
        var temp = counter * gain
        var remainder = temp and 0xF
        temp = temp shr 4

        if (remainder > 0 && !temp.bit7) {
            temp += if (counter < 0) -1 else 2
        }

        // 2. wrap if a certain range is exceeded
        if (temp >= 192) {
            temp -= 256
        } else if (temp < -64) {
            temp += 256
        }

        // 3. multiply result by pitch, then round to nearest while dropping 6 bits
        temp = (volumePitch * temp)
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

        counter = s.readInt("counter")
        modulationDisabled = s.readBoolean("modulationDisabled")
        s.readIntArrayOrFill("modTable", modTable, 0)
        modTablePosition = s.readInt("modTablePosition")
        overflowCounter = s.readInt("overflowCounter")
        output = s.readInt("output")
    }

    companion object {

        private const val RESET = 0xFF

        private val MOD_LUT = intArrayOf(0, 1, 2, 4, RESET, -4, -2, -1)
    }
}
