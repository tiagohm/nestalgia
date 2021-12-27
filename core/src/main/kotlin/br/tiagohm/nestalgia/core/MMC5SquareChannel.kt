package br.tiagohm.nestalgia.core

class MMC5SquareChannel(console: Console) : SquareChannel(AudioChannel.MMC5, console, null, false, true) {

    var output: Byte = 0
        private set

    init {
        output = 0
        reset(false)
    }

    override fun initializeSweep(value: UByte) {
        // $5001 has no effect. The MMC5 pulse channels will not sweep, as they have no sweep unit
    }

    fun run() {
        if (timer.toInt() == 0) {
            dutyPos = ((dutyPos - 1U) and 0x07U).toUByte()
            // Frequency values less than 8 do not silence the MMC5 pulse channels; they can output ultrasonic frequencies
            output = (DUTY_SEQUENCES[duty.toInt()][dutyPos.toInt()].toLong() * volume).toByte()
            timer = period
        } else {
            timer--
        }
    }
}