package br.tiagohm.nestalgia.core

class Apu(@JvmField internal val console: Console) : MemoryHandler, Resetable, Initializable, Snapshotable, Runnable {

    private var currentCycle = 0
    private var previousCycle = 0

    @JvmField internal val squareChannel1 = SquareChannel(AudioChannel.SQUARE_1, console, console.soundMixer, true)
    @JvmField internal val squareChannel2 = SquareChannel(AudioChannel.SQUARE_2, console, console.soundMixer, false)
    @JvmField internal val noiseChannel = NoiseChannel(AudioChannel.NOISE, console, console.soundMixer)
    @JvmField internal val triangleChannel = TriangleChannel(AudioChannel.TRIANGLE, console, console.soundMixer)
    @JvmField internal val deltaModulationChannel = DeltaModulationChannel(AudioChannel.DMC, console, console.soundMixer)
    @JvmField internal val frameCounter = ApuFrameCounter(console)

    @JvmField internal var enabled = true
    @JvmField internal var needToRun = false

    override fun initialize() {
        console.memoryManager.registerIODevice(squareChannel1)
        console.memoryManager.registerIODevice(squareChannel2)
        console.memoryManager.registerIODevice(frameCounter)
        console.memoryManager.registerIODevice(triangleChannel)
        console.memoryManager.registerIODevice(noiseChannel)
        console.memoryManager.registerIODevice(deltaModulationChannel)

        reset(false)

        updateRegion(console.region)
        console.soundMixer.updateRegion(console.region)
    }

    val dmcReadAddress
        get() = deltaModulationChannel.dmcReadAddress

    fun dmcReadBuffer(value: Int) {
        deltaModulationChannel.dmcReadBuffer(value)
    }

    fun updateRegion(region: Region) {
        // Finish the current apu frame before switching region.
        run()

        frameCounter.updateRegion(region)
    }

    override fun reset(softReset: Boolean) {
        enabled = true
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
        if (enabled) {
            exec()
        }
    }

    private fun exec() {
        currentCycle++

        if (currentCycle == SoundMixer.CYCLE_LENGTH - 1) {
            endFrame()
        } else if (needToRun(currentCycle)) {
            run()
        }
    }

    private fun needToRun(currentCycle: Int): Boolean {
        if (deltaModulationChannel.needToRun() || needToRun) {
            // Need to run whenever we alter the length counters
            // Need to run every cycle when DMC is running to get accurate emulation (CPU stalling, interaction with sprite DMA, etc.)
            needToRun = false
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

        console.soundMixer.playAudioBuffer(currentCycle)

        currentCycle = 0
        previousCycle = 0
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.READ, 0x4015)
        ranges.addHandler(MemoryOperation.WRITE, 0x4015)
    }

    val status: Int
        get() {
            var status = 0

            status = status or (if (squareChannel1.status) 0x01 else 0x00)
            status = status or (if (squareChannel2.status) 0x02 else 0x00)
            status = status or (if (triangleChannel.status) 0x04 else 0x00)
            status = status or (if (noiseChannel.status) 0x08 else 0x00)
            status = status or (if (deltaModulationChannel.status) 0x10 else 0x00)
            status = status or (if (console.cpu.hasIRQSource(IRQSource.FRAME_COUNTER)) 0x40 else 0x00)
            status = status or (if (console.cpu.hasIRQSource(IRQSource.DMC)) 0x80 else 0x00)

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

    override fun read(addr: Int, type: MemoryOperationType): Int {
        // $4015 read.
        run()

        // Reading $4015 clears the Frame Counter interrupt flag.
        return (status or console.memoryManager.openBus(0x20))
            .also { console.cpu.clearIRQSource(IRQSource.FRAME_COUNTER) }
    }

    override fun peek(addr: Int): Int {
        // Only run the APU (to catch up) if we're running this in the emulation thread
        // (not 100% accurate, but we can't run the APU from any other thread without locking)
        if (console.emulationThreadId == Thread.currentThread().id) {
            run()
        }

        return status
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        run()

        // Writing to $4015 clears the DMC interrupt flag.
        // This needs to be done before setting the enabled flag for the
        // DMC (because doing so can trigger an IRQ).
        console.cpu.clearIRQSource(IRQSource.DMC)

        squareChannel1.enabled = value.bit0
        squareChannel2.enabled = value.bit1
        triangleChannel.enabled = value.bit2
        noiseChannel.enabled = value.bit3
        deltaModulationChannel.enable(value.bit4)
    }

    override fun run() {
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
        console.soundMixer.addDelta(channel, currentCycle, delta)
    }

    override fun saveState(s: Snapshot) {
        // End the APU frame - makes it simpler to restore sound after a state reload.
        endFrame()

        s.write("square1", squareChannel1)
        s.write("square2", squareChannel2)
        s.write("triangle", triangleChannel)
        s.write("noise", noiseChannel)
        s.write("dmc", deltaModulationChannel)
        s.write("frameCounter", frameCounter)
        s.write("mixer", console.soundMixer)
    }

    override fun restoreState(s: Snapshot) {
        previousCycle = 0
        currentCycle = 0
        s.readSnapshotable("square1", squareChannel1)
        s.readSnapshotable("square2", squareChannel2)
        s.readSnapshotable("triangle", triangleChannel)
        s.readSnapshotable("noise", noiseChannel)
        s.readSnapshotable("dmc", deltaModulationChannel)
        s.readSnapshotable("frameCounter", frameCounter)
        s.readSnapshotable("mixer", console.soundMixer)
    }
}
