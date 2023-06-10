package br.tiagohm.nestalgia.core

open class FdsChannel : Memory, Snapshotable {

    protected var speed = 0
    protected var envelopeOff = false
    protected var volumeIncrease = false

    protected var timer = 0

    var gain = 0
        protected set

    var frequency = 0
        protected set

    // Few FDS NSFs write to this register. The BIOS initializes this to $E8.
    var masterSpeed = 0xE8
        internal set

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        when (addr and 0x03) {
            0 -> {
                speed = value and 0x3F
                volumeIncrease = value.bit6
                envelopeOff = value.bit7

                // Writing to this register immediately resets the clock timer that ticks the volume envelope (delaying the next tick slightly).
                resetTimer()

                if (envelopeOff) {
                    // Envelope is off, gain = speed
                    gain = speed
                }
            }
            2 -> frequency = (frequency and 0x0F00) or value
            3 -> frequency = (frequency and 0xFF) or (value and 0x0F shl 8)
        }
    }

    fun tickEnvelope(): Boolean {
        if (!envelopeOff && masterSpeed > 0) {
            timer--

            if (timer == 0) {
                resetTimer()

                if (volumeIncrease && gain < 32) {
                    gain++
                } else if (!volumeIncrease && gain > 0) {
                    gain--
                }

                return true
            }
        }

        return false
    }

    fun resetTimer() {
        timer = 8 * (speed + 1) * masterSpeed
    }

    override fun saveState(s: Snapshot) {
        s.write("speed", speed)
        s.write("gain", gain)
        s.write("envelopeOff", envelopeOff)
        s.write("volumeIncrease", volumeIncrease)
        s.write("frequency", frequency)
        s.write("timer", timer)
        s.write("masterSpeed", masterSpeed)
    }

    override fun restoreState(s: Snapshot) {
        speed = s.readInt("speed")
        gain = s.readInt("gain")
        envelopeOff = s.readBoolean("envelopeOff")
        volumeIncrease = s.readBoolean("volumeIncrease")
        frequency = s.readInt("frequency")
        timer = s.readInt("timer")
        masterSpeed = s.readInt("masterSpeed")
    }
}
