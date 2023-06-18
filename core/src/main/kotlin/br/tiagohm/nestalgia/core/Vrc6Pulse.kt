package br.tiagohm.nestalgia.core

class Vrc6Pulse : Memory, Clockable, Snapshotable {

    private var mVolume = 0
    private var dutyCycle = 0
    private var ignoreDuty = false
    private var frequency = 1
    private var enabled = false

    private var timer = 1
    private var step = 0

    @JvmField internal var frequencyShift = 0

    val volume
        get() = if (!enabled) 0
        else if (ignoreDuty) mVolume
        else if (step <= dutyCycle) mVolume
        else 0

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        when (addr and 0x03) {
            0 -> {
                mVolume = value and 0x0F
                dutyCycle = value and 0x70 shr 4
                ignoreDuty = value.bit7
            }
            1 -> frequency = frequency and 0x0F00 or value
            2 -> {
                frequency = frequency and 0xFF or (value and 0x0F shl 8)
                enabled = value.bit7

                if (!enabled) {
                    step = 0
                }
            }
        }
    }

    override fun clock() {
        if (enabled) {
            timer--

            if (timer <= 0) {
                step = step + 1 and 0x0F
                timer = (frequency shr frequencyShift) + 1
            }
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("volume", mVolume)
        s.write("dutyCycle", dutyCycle)
        s.write("ignoreDuty", ignoreDuty)
        s.write("frequency", frequency)
        s.write("enabled", enabled)

        s.write("timer", timer)
        s.write("step", step)
        s.write("frequencyShift", frequencyShift)
    }

    override fun restoreState(s: Snapshot) {
        mVolume = s.readInt("volume")
        dutyCycle = s.readInt("dutyCycle")
        ignoreDuty = s.readBoolean("ignoreDuty")
        frequency = s.readInt("frequency", 1)
        enabled = s.readBoolean("enabled")

        timer = s.readInt("timer", 1)
        step = s.readInt("step")
        frequencyShift = s.readInt("frequencyShift")
    }
}
