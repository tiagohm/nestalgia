package br.tiagohm.nestalgia.core

abstract class ApuEnvelope(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer?,
) : ApuLengthCounter(channel, console, mixer) {

    @Volatile private var constantVolume = false
    @Volatile private var start = false
    @Volatile private var divider = 0
    @Volatile private var counter = 0
    @Volatile private var mVolume = 0

    fun initializeEnvelope(regValue: Int) {
        constantVolume = regValue.bit4
        mVolume = regValue and 0x0F
    }

    fun resetEnvelope() {
        start = true
    }

    override val volume: Int
        get() {
            return if (lengthCounter > 0) {
                if (constantVolume) {
                    mVolume
                } else {
                    counter
                }
            } else {
                0
            }
        }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        constantVolume = false
        mVolume = 0
        start = false
        divider = 0
        counter = 0
    }

    fun tickEnvelope() {
        if (!start) {
            divider--

            if (divider < 0) {
                divider = mVolume

                if (counter > 0) {
                    counter--
                } else if (lengthCounterHalt) {
                    counter = 15
                }
            }
        } else {
            start = false
            counter = 15
            divider = mVolume
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("constantVolume", constantVolume)
        s.write("volume", mVolume)
        s.write("start", start)
        s.write("divider", divider)
        s.write("counter", counter)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        constantVolume = s.readBoolean("constantVolume")
        mVolume = s.readInt("volume")
        start = s.readBoolean("start")
        divider = s.readInt("divider")
        counter = s.readInt("counter")
    }
}
