package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
open class FdsChannel :
    Memory,
    Snapshotable {

    protected var speed: UByte = 0U
    protected var envelopeOff = false
    protected var volumeIncrease = false

    protected var timer: UInt = 0U

    var gain: UByte = 0U
        protected set

    var frequency: UShort = 0U
        protected set

    // Few FDS NSFs write to this register. The BIOS initializes this to $FF
    var masterSpeed: UByte = 0xFFU

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        when (addr.toInt() and 0x03) {
            0 -> {
                speed = value and 0x3FU
                volumeIncrease = value.bit6
                envelopeOff = value.bit7

                // Writing to this register immediately resets the clock timer that ticks the volume envelope (delaying the next tick slightly).
                resetTimer()

                if (envelopeOff) {
                    // Envelope is off, gain = speed
                    gain = speed
                }
            }
            2 -> frequency = (frequency and 0x0F00U) or value.toUShort()
            3 -> frequency = (frequency and 0xFFU) or ((value.toUInt() and 0x0FU) shl 8).toUShort()
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte = 0U

    fun tickEnvelope(): Boolean {
        if (!envelopeOff && masterSpeed > 0U) {
            timer--

            if (timer == 0U) {
                resetTimer()

                if (volumeIncrease && gain < 32U) {
                    gain++
                } else if (!volumeIncrease && gain > 0U) {
                    gain--
                }

                return true
            }
        }

        return false
    }

    fun resetTimer() {
        timer = 8U * (speed + 1U) * masterSpeed
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
        s.load()

        speed = s.readUByte("speed") ?: 0U
        gain = s.readUByte("gain") ?: 0U
        envelopeOff = s.readBoolean("envelopeOff") ?: false
        volumeIncrease = s.readBoolean("volumeIncrease") ?: false
        frequency = s.readUShort("frequency") ?: 0U
        timer = s.readUInt("timer") ?: 0U
        masterSpeed = s.readUByte("masterSpeed") ?: 0U
    }
}
