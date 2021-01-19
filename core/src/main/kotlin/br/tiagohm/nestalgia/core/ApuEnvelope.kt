package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
abstract class ApuEnvelope(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer,
) : ApuLengthCounter(channel, console, mixer) {

    private var constantVolume = false
    private var envelopeCounter: UByte = 0U
    private var start = false
    private var divider: Byte = 0
    private var counter: UByte = 0U
    private var privateVolume: UByte = 0U

    fun initializeEnvelope(regValue: UByte) {
        constantVolume = regValue.bit4
        privateVolume = regValue and 0x0FU
    }

    fun resetEnvelope() {
        start = true
    }

    override val volume: Long
        get() {
            return if (lengthCounter > 0U) {
                if (constantVolume) {
                    privateVolume.toLong()
                } else {
                    counter.toLong()
                }
            } else {
                0L
            }
        }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        constantVolume = false
        privateVolume = 0U
        envelopeCounter = 0U
        start = false
        divider = 0
        counter = 0U
    }

    fun tickEnvelope() {
        if (!start) {
            divider--

            if (divider < 0) {
                divider = privateVolume.toByte()
                if (counter > 0U) {
                    counter--
                } else if (lengthCounterHalt) {
                    counter = 15U
                }
            }
        } else {
            start = false
            counter = 15U
            divider = privateVolume.toByte()
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("constantVolume", constantVolume)
        s.write("privateVolume", privateVolume)
        s.write("envelopeCounter", envelopeCounter)
        s.write("start", start)
        s.write("divider", divider)
        s.write("counter", counter)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        constantVolume = s.readBoolean("constantVolume") ?: false
        privateVolume = s.readUByte("privateVolume") ?: 0U
        envelopeCounter = s.readUByte("envelopeCounter") ?: 0U
        start = s.readBoolean("start") ?: false
        divider = s.readByte("divider") ?: 0
        counter = s.readUByte("counter") ?: 0U
    }
}