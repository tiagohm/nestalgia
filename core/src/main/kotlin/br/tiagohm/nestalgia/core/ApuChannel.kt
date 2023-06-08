package br.tiagohm.nestalgia.core

abstract class ApuChannel(
    protected val channel: AudioChannel,
    protected val console: Console,
    private val mixer: SoundMixer? = null,
) : MemoryHandler, Resetable, Snapshotable {

    private var previousCycle = 0
    protected var lastOutput = 0

    var timer = 0
        protected set

    open var period = 0
        protected set

    private var mRegion = Region.NTSC

    var region: Region
        get() = if (mRegion == Region.DENDY) Region.NTSC else mRegion
        set(value) {
            mRegion = value
        }

    override fun reset(softReset: Boolean) {
        timer = 0
        period = 0
        lastOutput = 0
        previousCycle = 0
    }

    protected abstract fun clock()

    abstract val status: Boolean

    abstract val frequency: Double

    abstract val volume: Int

    abstract val enabled: Boolean

    abstract val muted: Boolean

    fun run(targetCycle: Int) {
        var cyclesToRun = targetCycle - previousCycle

        while (cyclesToRun > timer) {
            cyclesToRun -= timer + 1
            previousCycle += timer + 1
            clock()
            timer = period
        }

        timer -= cyclesToRun
        previousCycle = targetCycle
    }

    open fun addOutput(output: Int) {
        if (output != lastOutput) {
            mixer?.addDelta(channel, previousCycle, output - lastOutput)
            lastOutput = output
        }
    }

    fun endFrame() {
        previousCycle = 0
    }

    override fun saveState(s: Snapshot) {
        s.write("lastOutput", lastOutput)
        s.write("timer", timer)
        s.write("period", period)
        s.write("region", mRegion)
    }

    override fun restoreState(s: Snapshot) {
        previousCycle = 0
        lastOutput = s.readInt("lastOutput")
        timer = s.readInt("timer")
        period = s.readInt("period")
        mRegion = s.readEnum("region", Region.AUTO)
    }
}
