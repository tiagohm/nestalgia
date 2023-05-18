package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
abstract class ApuLengthCounter(
    channel: AudioChannel,
    console: Console,
    mixer: SoundMixer?,
) : ApuChannel(channel, console, mixer) {

    protected var newHaltValue = false

    protected var lengthCounterHalt = false
    protected var lengthCounter: UByte = 0U
    protected var lengthCounterReloadValue: UByte = 0U
    protected var lengthCounterPreviousValue: UByte = 0U

    override var isEnabled = false
        set(value) {
            if (!value) lengthCounter = 0U
            field = value
        }

    protected inline fun initializeLengthCounter(haltFlag: Boolean) {
        console.apu.isNeedToRun = true
        newHaltValue = haltFlag
    }

    protected inline fun loadLengthCounter(value: UByte) {
        if (isEnabled) {
            lengthCounterReloadValue = LC_LOOKUP_TABLE[value.toInt()]
            lengthCounterPreviousValue = lengthCounter
            console.apu.isNeedToRun = true
        }
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        if (softReset) {
            isEnabled = false

            if (channel != AudioChannel.TRIANGLE) {
                // At reset, length counters should be enabled, triangle unaffected
                lengthCounterHalt = false
                lengthCounter = 0U
                newHaltValue = false
                lengthCounterReloadValue = 0U
                lengthCounterPreviousValue = 0U
            }
        } else {
            isEnabled = false
            lengthCounterHalt = false
            lengthCounter = 0U
            newHaltValue = false
            lengthCounterReloadValue = 0U
            lengthCounterPreviousValue = 0U
        }
    }

    override val status: Boolean
        get() = lengthCounter.toUInt() > 0U

    fun reloadCounter() {
        if (lengthCounterReloadValue.isNonZero) {
            if (lengthCounter == lengthCounterPreviousValue) {
                lengthCounter = lengthCounterReloadValue
            }

            lengthCounterReloadValue = 0U
        }

        lengthCounterHalt = newHaltValue
    }

    fun tickLengthCounter() {
        if (lengthCounter > 0U && !lengthCounterHalt) {
            lengthCounter--
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("isEnabled", isEnabled)
        s.write("lengthCounterHalt", lengthCounterHalt)
        s.write("newHaltValue", newHaltValue)
        s.write("lengthCounter", lengthCounter)
        s.write("lengthCounterPreviousValue", lengthCounterPreviousValue)
        s.write("lengthCounterReloadValue", lengthCounterReloadValue)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        isEnabled = s.readBoolean("isEnabled") ?: false
        lengthCounterHalt = s.readBoolean("lengthCounterHalt") ?: false
        newHaltValue = s.readBoolean("newHaltValue") ?: false
        lengthCounter = s.readUByte("lengthCounter") ?: 0U
        lengthCounterPreviousValue = s.readUByte("lengthCounterPreviousValue") ?: 0U
        lengthCounterReloadValue = s.readUByte("lengthCounterReloadValue") ?: 0U
    }

    companion object {

        @JvmStatic protected val LC_LOOKUP_TABLE = ubyteArrayOf(
            10U, 254U, 20U, 2U, 40U, 4U, 80U, 6U,
            160U, 8U, 60U, 10U, 14U, 12U, 26U, 14U,
            12U, 16U, 24U, 18U, 48U, 20U, 96U, 22U,
            192U, 24U, 72U, 26U, 16U, 28U, 32U, 30U,
        )
    }
}
