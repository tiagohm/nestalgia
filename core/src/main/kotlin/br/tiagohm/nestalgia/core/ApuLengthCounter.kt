package br.tiagohm.nestalgia.core

abstract class ApuLengthCounter(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer?,
) : ApuChannel(channel, console, mixer) {

    @JvmField @Volatile protected var newHaltValue = false
    @JvmField @Volatile protected var lengthCounterHalt = false
    @JvmField @Volatile protected var lengthCounter = 0
    @JvmField @Volatile protected var lengthCounterReloadValue = 0
    @JvmField @Volatile protected var lengthCounterPreviousValue = 0

    override var enabled = false
        internal set(value) {
            if (!value) lengthCounter = 0
            field = value
        }

    protected fun initializeLengthCounter(haltFlag: Boolean) {
        console.apu.needToRun = true
        newHaltValue = haltFlag
    }

    protected fun loadLengthCounter(value: Int) {
        if (enabled) {
            lengthCounterReloadValue = LC_LOOKUP_TABLE[value]
            lengthCounterPreviousValue = lengthCounter
            console.apu.needToRun = true
        }
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        if (softReset) {
            enabled = false

            if (channel != AudioChannel.TRIANGLE) {
                // At reset, length counters should be enabled, triangle unaffected.
                lengthCounterHalt = false
                lengthCounter = 0
                newHaltValue = false
                lengthCounterReloadValue = 0
                lengthCounterPreviousValue = 0
            }
        } else {
            enabled = false
            lengthCounterHalt = false
            lengthCounter = 0
            newHaltValue = false
            lengthCounterReloadValue = 0
            lengthCounterPreviousValue = 0
        }
    }

    override val status
        get() = lengthCounter > 0

    fun reloadCounter() {
        if (lengthCounterReloadValue != 0) {
            if (lengthCounter == lengthCounterPreviousValue) {
                lengthCounter = lengthCounterReloadValue
            }

            lengthCounterReloadValue = 0
        }

        lengthCounterHalt = newHaltValue
    }

    fun tickLengthCounter() {
        if (lengthCounter > 0 && !lengthCounterHalt) {
            lengthCounter--
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("enabled", enabled)
        s.write("lengthCounterHalt", lengthCounterHalt)
        s.write("newHaltValue", newHaltValue)
        s.write("lengthCounter", lengthCounter)
        s.write("lengthCounterPreviousValue", lengthCounterPreviousValue)
        s.write("lengthCounterReloadValue", lengthCounterReloadValue)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        enabled = s.readBoolean("enabled")
        lengthCounterHalt = s.readBoolean("lengthCounterHalt")
        newHaltValue = s.readBoolean("newHaltValue")
        lengthCounter = s.readInt("lengthCounter")
        lengthCounterPreviousValue = s.readInt("lengthCounterPreviousValue")
        lengthCounterReloadValue = s.readInt("lengthCounterReloadValue")
    }

    companion object {

        private val LC_LOOKUP_TABLE = intArrayOf(
            10, 254, 20, 2, 40, 4, 80, 6,
            160, 8, 60, 10, 14, 12, 26, 14,
            12, 16, 24, 18, 48, 20, 96, 22,
            192, 24, 72, 26, 16, 28, 32, 30,
        )
    }
}
