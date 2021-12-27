package br.tiagohm.nestalgia.core

abstract class ApuChannel(
    val channel: AudioChannel,
    val console: Console,
    val mixer: SoundMixer? = null,
) : MemoryHandler,
    Resetable,
    Snapshotable {

    private var previousCycle = 0
    protected var lastOutput: Byte = 0

    var timer: UShort = 0U
        protected set

    open var period: UShort = 0U
        protected set

    private var privateRegion = Region.NTSC
    var region: Region
        get() = if (privateRegion == Region.DENDY) Region.NTSC else privateRegion
        set(value) {
            privateRegion = value
        }

    init {
        reset(false)
    }

    override fun reset(softReset: Boolean) {
        timer = 0U
        period = 0U
        lastOutput = 0
        previousCycle = 0
    }

    abstract fun clock()

    abstract val status: Boolean

    abstract val frequency: Double

    abstract val volume: Long

    abstract val isEnabled: Boolean

    fun run(targetCycle: Int) {
        var cyclesToRun = targetCycle - previousCycle

        while (cyclesToRun > timer.toInt()) {
            cyclesToRun -= (timer + 1U).toInt()
            previousCycle += (timer + 1U).toInt()
            clock()
            timer = period
        }

        timer = (timer.toInt() - cyclesToRun).toUShort()
        previousCycle = targetCycle
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte = 0U

    open fun addOutput(output: Byte) {
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
        s.write("region", privateRegion)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        previousCycle = 0
        lastOutput = s.readByte("lastOutput") ?: 0
        timer = s.readUShort("timer") ?: 0U
        period = s.readUShort("period") ?: 0U
        privateRegion = s.readEnum<Region>("region") ?: Region.AUTO
    }
}