package br.tiagohm.nestalgia.core

class Vrc6Saw : Memory, Clockable, Snapshotable {

    @Volatile private var accumulatorRate = 0
    @Volatile private var accumulator = 0
    @Volatile private var frequency = 1
    @Volatile private var enabled = false

    @Volatile private var timer = 1
    @Volatile private var step = 0
    @JvmField internal var frequencyShift = 0

    val volume
        get() = if (!enabled) 0
        // The high 5 bits of the accumulator are then output
        // (provided the channel is enabled by having the E bit set).
        else accumulator shr 3

    override fun clock() {
        if (enabled) {
            timer--

            if (timer <= 0) {
                step = (step + 1) % 14
                timer = (frequency shr frequencyShift) + 1

                if (step == 0) {
                    accumulator = 0
                } else if (!step.bit0) {
                    accumulator += accumulatorRate
                }
            }
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        when (addr and 0x03) {
            0 -> accumulatorRate = value and 0x3F
            1 -> frequency = frequency and 0x0F00 or value
            2 -> {
                frequency = frequency and 0xFF or (value and 0x0F shl 8)
                enabled = value.bit7

                if (!enabled) {
                    // If E is clear, the accumulator is forced to zero until E is again set.
                    accumulator = 0

                    // The phase of the saw generator can be mostly reset by clearing and
                    // immediately setting E. Clearing E does not reset the frequency divider,
                    // however.
                    step = 0
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("accumulatorRate", accumulatorRate)
        s.write("accumulator", accumulator)
        s.write("frequency", frequency)
        s.write("enabled", enabled)

        s.write("timer", timer)
        s.write("step", step)
        s.write("frequencyShift", frequencyShift)
    }

    override fun restoreState(s: Snapshot) {
        accumulatorRate = s.readInt("accumulatorRate")
        accumulator = s.readInt("accumulator")
        frequency = s.readInt("frequency", 1)
        enabled = s.readBoolean("enabled")
        timer = s.readInt("timer", 1)
        step = s.readInt("step")
        frequencyShift = s.readInt("frequencyShift")
    }
}
