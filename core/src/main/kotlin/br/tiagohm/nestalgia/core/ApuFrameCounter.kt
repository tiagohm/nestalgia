package br.tiagohm.nestalgia.core

class ApuFrameCounter(val console: Console) :
    MemoryHandler,
    Resetable,
    Snapshotable {

    private val stepCycles = Array(2) { IntArray(6) }
    private var previousCycle = 0
    private var currentStep = 0
    private var stepMode = false // 0: 4-step mode, 1: 5-step mode
    private var inhibitIRQ = false
    private var blockFrameCounterTick: UByte = 0U
    private var newValue: Short = 0
    private var writeDelayCounter: Byte = 0

    private var privateRegion = Region.AUTO
    var region: Region
        get() = privateRegion
        set(value) {
            if (value != Region.AUTO) {
                privateRegion = value
                updateStepCycles(value)
            }
        }

    init {
        reset(false)
    }

    private fun updateStepCycles(region: Region) {
        when (region) {
            Region.NTSC,
            Region.DENDY -> {
                STEP_CYCLES_NTSC[0].copyInto(stepCycles[0])
                STEP_CYCLES_NTSC[1].copyInto(stepCycles[1])
            }
            Region.PAL -> {
                STEP_CYCLES_PAL[0].copyInto(stepCycles[0])
                STEP_CYCLES_PAL[1].copyInto(stepCycles[1])
            }
            else -> {
                System.err.println("ERROR: AUTO should never be set here")
            }
        }
    }

    override fun reset(softReset: Boolean) {
        previousCycle = 0

        // After reset: APU mode in $4017 was unchanged, so we need to keep whatever value _stepMode has for soft resets
        if (!softReset) {
            stepMode = false
        }

        currentStep = 0

        // After reset or power-up, APU acts as if $4017 were written with $00 from 9 to 12 clocks before first instruction begins.
        // This is emulated in the CPU::Reset function
        // Reset acts as if $00 was written to $4017
        newValue = if (stepMode) 0x80 else 0x00
        writeDelayCounter = 3
        inhibitIRQ = false

        blockFrameCounterTick = 0U
    }

    fun run(cycles: Int): Pair<Int, Int> {
        var cyclesToRun = cycles
        val cyclesRan: Int

        if (previousCycle + cyclesToRun >= stepCycles[stepMode.toInt()][currentStep]) {
            if (!inhibitIRQ && !stepMode && currentStep >= 3) {
                // Set IRQ on the last 3 cycles for 4-step mode
                console.cpu.setIRQSource(IRQSource.FRAME_COUNTER)
            }

            val type = FRAME_TYPE[stepMode.toInt()][currentStep]

            if (type != FrameType.NONE && blockFrameCounterTick.isZero) {
                console.apu.frameCounterTick(type)
                // Do not allow writes to 4017 to clock the frame counter for the next cycle (i.e this odd cycle + the following even cycle)
                blockFrameCounterTick = 2U
            }

            cyclesRan = if (stepCycles[stepMode.toInt()][currentStep] < previousCycle) {
                // This can happen when switching from PAL to NTSC, which can cause a freeze (endless loop in APU)
                0
            } else {
                stepCycles[stepMode.toInt()][currentStep] - previousCycle
            }

            cyclesToRun -= cyclesRan

            if (++currentStep == 6) {
                currentStep = 0
                previousCycle = 0
            } else {
                previousCycle += cyclesRan
            }
        } else {
            cyclesRan = cyclesToRun
            cyclesToRun = 0
            previousCycle += cyclesRan
        }

        if (newValue >= 0) {
            writeDelayCounter--

            if (writeDelayCounter.toInt() == 0) {
                // Apply new value after the appropriate number of cycles has elapsed
                stepMode = (newValue.toInt() and 0x80) == 0x80

                writeDelayCounter = -1
                currentStep = 0
                previousCycle = 0
                newValue = -1

                if (stepMode && blockFrameCounterTick.isZero) {
                    // Writing to $4017 with bit 7 set will immediately generate a clock for both the quarter frame and the half frame units, regardless of what the sequencer is doing."
                    console.apu.frameCounterTick(FrameType.HALF)
                    blockFrameCounterTick = 2U
                }
            }
        }

        if (blockFrameCounterTick > 0U) {
            blockFrameCounterTick--
        }

        return cyclesRan to cyclesToRun
    }

    fun needToRun(cycles: Int): Boolean {
        // Run APU when:
        // - A new value is pending
        // - The "blockFrameCounterTick" process is running
        // - We're at the before-last or last tick of the current step
        return newValue >= 0 ||
            blockFrameCounterTick > 0U ||
            (previousCycle + cycles) >= stepCycles[stepMode.toInt()][currentStep] - 1
    }

    override fun getMemoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(MemoryOperation.WRITE, 0x4017U)
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte = 0U

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        console.apu.run()

        newValue = value.toShort()

        writeDelayCounter = if ((console.cpu.cycleCount and 0x01L) == 0x01L) {
            // If the write occurs between APU cycles, the effects occur 4 CPU cycles after the write cycle.
            4
        } else {
            // If the write occurs during an APU cycle, the effects occur 3 CPU cycles after the $4017 write cycle
            3
        }

        inhibitIRQ = value.bit6

        if (inhibitIRQ) {
            console.cpu.clearIRQSource(IRQSource.FRAME_COUNTER)
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("previousCycle", previousCycle)
        s.write("currentStep", currentStep)
        s.write("stepMode", stepMode)
        s.write("inhibitIRQ", inhibitIRQ)
        s.write("privateRegion", privateRegion)
        s.write("blockFrameCounterTick", blockFrameCounterTick)
        s.write("writeDelayCounter", writeDelayCounter)
        s.write("newValue", newValue)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        previousCycle = s.readInt("previousCycle") ?: 0
        currentStep = s.readInt("currentStep") ?: 0
        stepMode = s.readBoolean("stepMode") ?: false
        inhibitIRQ = s.readBoolean("inhibitIRQ") ?: false
        region = s.readEnum<Region>("region") ?: Region.AUTO
        blockFrameCounterTick = s.readUByte("blockFrameCounterTick") ?: 0U
        writeDelayCounter = s.readByte("writeDelayCounter") ?: 0
        newValue = s.readShort("newValue") ?: 0
    }

    companion object {

        @JvmStatic private val STEP_CYCLES_NTSC = arrayOf(
            intArrayOf(7457, 14913, 22371, 29828, 29829, 29830),
            intArrayOf(7457, 14913, 22371, 29829, 37281, 37282),
        )

        @JvmStatic private val STEP_CYCLES_PAL = arrayOf(
            intArrayOf(8313, 16627, 24939, 33252, 33253, 33254),
            intArrayOf(8313, 16627, 24939, 33253, 41565, 41566),
        )

        @JvmStatic private val FRAME_TYPE = arrayOf(
            arrayOf(
                FrameType.QUARTER,
                FrameType.HALF,
                FrameType.QUARTER,
                FrameType.NONE,
                FrameType.HALF,
                FrameType.NONE
            ),
            arrayOf(
                FrameType.QUARTER,
                FrameType.HALF,
                FrameType.QUARTER,
                FrameType.NONE,
                FrameType.HALF,
                FrameType.NONE
            ),
        )
    }
}
