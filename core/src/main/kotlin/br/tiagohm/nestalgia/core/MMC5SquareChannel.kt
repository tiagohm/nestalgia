package br.tiagohm.nestalgia.core

internal class MMC5SquareChannel(console: Console) : SquareChannel(AudioChannel.MMC5, console, null, false, true), Runnable {

    @JvmField var output = 0

    init {
        output = 0
        reset(false)
    }

    override fun initializeSweep(value: Int) {
        // $5001 has no effect. The MMC5 pulse channels will not sweep, as they have no sweep unit.
    }

    override fun run() {
        if (timer <= 0) {
            dutyPos = (dutyPos - 1) and 0x07
            // Frequency values less than 8 do not silence the MMC5 pulse channels; they can output ultrasonic frequencies
            output = DUTY_SEQUENCES[duty][dutyPos] * volume
            timer = period
        } else {
            timer--
        }
    }
}
