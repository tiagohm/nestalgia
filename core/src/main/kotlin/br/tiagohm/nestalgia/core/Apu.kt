package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Apu(val console: Console) :
    MemoryHandler,
    Resetable,
    Snapshotable {

    private val mixer = console.soundMixer
    private var currentCycle = 0
    private var previousCycle = 0

    val squareChannel1 = SquareChannel(AudioChannel.SQUARE_1, console, mixer, true)
    val squareChannel2 = SquareChannel(AudioChannel.SQUARE_2, console, mixer, false)
    val noiseChannel = NoiseChannel(AudioChannel.NOISE, console, mixer)
    val triangleChannel = TriangleChannel(AudioChannel.TRIANGLE, console, mixer)
    val deltaModulationChannel = DeltaModulationChannel(AudioChannel.DMC, console, mixer)
    val frameCounter = ApuFrameCounter(console)

    var isEnabled = true
    var isNeedToRun = false

    init {
        console.memoryManager.registerIODevice(squareChannel1)
        console.memoryManager.registerIODevice(squareChannel2)
        console.memoryManager.registerIODevice(frameCounter)
        console.memoryManager.registerIODevice(triangleChannel)
        console.memoryManager.registerIODevice(noiseChannel)
        console.memoryManager.registerIODevice(deltaModulationChannel)

        reset(false)
    }

    inline val dmcReadAddress: UShort
        get() = deltaModulationChannel.dmcReadAddress

    inline fun setDmcReadBuffer(value: UByte) {
        deltaModulationChannel.setDmcReadBuffer(value)
    }

    private var privateRegion = Region.AUTO
    var region: Region
        get() = privateRegion
        set(value) {
            // Finish the current apu frame before switching region
            run()

            privateRegion = value

            squareChannel1.region = region
            squareChannel2.region = region
            triangleChannel.region = region
            noiseChannel.region = region
            deltaModulationChannel.region = region
            frameCounter.region = region

            mixer.region = region
        }

    override fun reset(softReset: Boolean) {
        isEnabled = true
        currentCycle = 0
        previousCycle = 0

        squareChannel1.reset(softReset)
        squareChannel2.reset(softReset)
        triangleChannel.reset(softReset)
        noiseChannel.reset(softReset)
        deltaModulationChannel.reset(softReset)
        frameCounter.reset(softReset)
    }

    fun processCpuClock() {
        if (isEnabled) {
            exec()
        }
    }

    private inline fun exec() {
        currentCycle++

        if (currentCycle == SoundMixer.CYCLE_LENGTH - 1) {
            endFrame()
        } else if (needToRun(currentCycle)) {
            run()
        }
    }

    private inline fun needToRun(currentCycle: Int): Boolean {
        if (deltaModulationChannel.needToRun() || isNeedToRun) {
            // Need to run whenever we alter the length counters
            // Need to run every cycle when DMC is running to get accurate emulation (CPU stalling, interaction with sprite DMA, etc.)
            isNeedToRun = false
            return true
        }

        val cyclesToRun = currentCycle - previousCycle

        return frameCounter.needToRun(cyclesToRun) || deltaModulationChannel.irqPending(cyclesToRun)
    }

    fun endFrame() {
        run()

        squareChannel1.endFrame()
        squareChannel2.endFrame()
        triangleChannel.endFrame()
        noiseChannel.endFrame()
        deltaModulationChannel.endFrame()

        mixer.playAudioBuffer(currentCycle)

        currentCycle = 0
        previousCycle = 0
    }

    override fun getMemoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.READ, 0x4015U)
        ranges.addHandler(MemoryOperation.WRITE, 0x4015U)
    }

    val status: UByte
        get() {
            var status: UByte = 0U

            status = status or (if (squareChannel1.status) 0x01U else 0x00U)
            status = status or (if (squareChannel2.status) 0x02U else 0x00U)
            status = status or (if (triangleChannel.status) 0x04U else 0x00U)
            status = status or (if (noiseChannel.status) 0x08U else 0x00U)
            status = status or (if (deltaModulationChannel.status) 0x10U else 0x00U)
            status = status or (if (console.cpu.hasIRQSource(IRQSource.FRAME_COUNTER)) 0x40U else 0x00U)
            status = status or (if (console.cpu.hasIRQSource(IRQSource.DMC)) 0x80U else 0x00U)

            return status
        }

    fun frameCounterTick(type: FrameType) {
        // Quarter & half frame clock envelope & linear counter
        squareChannel1.tickEnvelope()
        squareChannel2.tickEnvelope()
        triangleChannel.tickLinearCounter()
        noiseChannel.tickEnvelope()

        if (type == FrameType.HALF) {
            // Half frames clock length counter & sweep
            squareChannel1.tickLengthCounter()
            squareChannel2.tickLengthCounter()
            triangleChannel.tickLengthCounter()
            noiseChannel.tickLengthCounter()

            squareChannel1.tickSweep()
            squareChannel2.tickSweep()
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        run()

        val status = this.status

        // Reading $4015 clears the Frame Counter interrupt flag.
        console.cpu.clearIRQSource(IRQSource.FRAME_COUNTER)

        return status
    }

    override fun peek(addr: UShort): UByte {
        // Only run the APU (to catch up) if we're running this in the emulation thread
        // (not 100% accurate, but we can't run the APU from any other thread without locking)
        if (console.emulationThreadId == Thread.currentThread().id) {
            run()
        }

        return status
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        run()

        // Writing to $4015 clears the DMC interrupt flag.
        // This needs to be done before setting the enabled flag for the DMC (because doing so can trigger an IRQ)
        console.cpu.clearIRQSource(IRQSource.DMC)

        squareChannel1.isEnabled = value.bit0
        squareChannel2.isEnabled = value.bit1
        triangleChannel.isEnabled = value.bit2
        noiseChannel.isEnabled = value.bit3
        deltaModulationChannel.setEnabled(value.bit4)
    }

    fun run() {
        // Update framecounter and all channels
        // This is called:
        // - At the end of a frame
        // - Before APU registers are read/written to
        // - When a DMC or FrameCounter interrupt needs to be fired

        var cyclesToRun = currentCycle - previousCycle

        while (cyclesToRun > 0) {
            val (a, b) = frameCounter.run(cyclesToRun)

            previousCycle += a
            cyclesToRun = b

            // Reload counters set by writes to 4003/4008/400B/400F after running the frame counter to allow the length counter to be clocked first
            // This fixes the test "len_reload_timing" (tests 4 & 5)

            squareChannel1.reloadCounter()
            squareChannel2.reloadCounter()
            noiseChannel.reloadCounter()
            triangleChannel.reloadCounter()

            squareChannel1.run(previousCycle)
            squareChannel2.run(previousCycle)
            noiseChannel.run(previousCycle)
            triangleChannel.run(previousCycle)
            deltaModulationChannel.run(previousCycle)
        }
    }

    fun addExpansionAudioDelta(channel: AudioChannel, delta: Int) {
        mixer.addDelta(channel, currentCycle, delta)
    }

    override fun saveState(s: Snapshot) {
        // End the APU frame - makes it simpler to restore sound after a state reload
        endFrame()

        s.write("region", privateRegion)
        s.write("square1", squareChannel1)
        s.write("square2", squareChannel2)
        s.write("triangle", triangleChannel)
        s.write("noise", noiseChannel)
        s.write("dmc", deltaModulationChannel)
        s.write("frameCounter", frameCounter)
        s.write("mixer", mixer)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        previousCycle = 0
        currentCycle = 0
        privateRegion = s.readEnum<Region>("region") ?: Region.AUTO
        s.readSnapshot("square1")?.let { squareChannel1.restoreState(it) }
        s.readSnapshot("square2")?.let { squareChannel2.restoreState(it) }
        s.readSnapshot("triangle")?.let { triangleChannel.restoreState(it) }
        s.readSnapshot("noise")?.let { noiseChannel.restoreState(it) }
        s.readSnapshot("dmc")?.let { deltaModulationChannel.restoreState(it) }
        s.readSnapshot("frameCounter")?.let { frameCounter.restoreState(it) }
        s.readSnapshot("mixer")?.let { mixer.restoreState(it) }
    }
}
