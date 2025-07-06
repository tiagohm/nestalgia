package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.WRITE

class ApuFrameCounter(private val console: Console) : MemoryHandler, Resetable, Snapshotable {

    private val stepCycles = Array(2) { IntArray(6) }
    @Volatile private var previousCycle = 0
    @Volatile private var currentStep = 0
    @Volatile private var stepMode = false // 0: 4-step mode, 1: 5-step mode
    @Volatile private var inhibitIRQ = false
    @Volatile private var blockFrameCounterTick = 0
    @Volatile private var newValue = 0
    @Volatile private var writeDelayCounter = 0
    @Volatile private var irqFlag = false
    @Volatile private var irqFlagClearClock = 0L

    init {
        reset(false)
    }

    fun updateRegion(region: Region) {
        if (region != Region.AUTO) {
            updateStepCycles(region)
        }
    }

    fun hasIrqFlag(): Boolean {
        if (irqFlag) {
            val clock = console.masterClock

            if (irqFlagClearClock == 0L) {
                // The flag will be cleared at the start of the next APU cycle (see AccuracyCoin test)
                irqFlagClearClock = clock + if (clock.bit0) 2L else 1L
            } else if (clock >= irqFlagClearClock) {
                irqFlagClearClock = 0
                irqFlag = false
            }
        }

        return irqFlag
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
        irqFlag = false
        irqFlagClearClock = 0L

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

        blockFrameCounterTick = 0
    }

    fun run(cycles: Int): IntArray {
        var cyclesToRun = cycles
        val cyclesRan: Int

        if (previousCycle + cyclesToRun >= stepCycles[stepMode.toInt()][currentStep]) {
            if (!stepMode && currentStep >= 3) {
                // Set irq on the last 3 cycles for 4-step mode
                irqFlag = true
                irqFlagClearClock = 0L

                if (!inhibitIRQ) {
                    console.cpu.setIRQSource(IRQSource.FRAME_COUNTER)
                } else if (currentStep == 5) {
                    irqFlag = false
                    irqFlagClearClock = 0
                }
            }

            val type = FRAME_TYPE[stepMode.toInt()][currentStep]

            if (type != FrameType.NONE && blockFrameCounterTick == 0) {
                console.apu.frameCounterTick(type)
                // Do not allow writes to 4017 to clock the frame counter for the
                // next cycle (i.e this odd cycle + the following even cycle).
                blockFrameCounterTick = 2
            }

            cyclesRan = if (stepCycles[stepMode.toInt()][currentStep] < previousCycle) {
                // This can happen when switching from PAL to NTSC, which can cause
                // a freeze (endless loop in APU).
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

            if (writeDelayCounter <= 0) {
                // Apply new value after the appropriate number of cycles has elapsed.
                stepMode = newValue.bit7

                writeDelayCounter = -1
                currentStep = 0
                previousCycle = 0
                newValue = -1

                if (stepMode && blockFrameCounterTick == 0) {
                    // Writing to $4017 with bit 7 set will immediately generate a clock for both the quarter frame and the half frame units, regardless of what the sequencer is doing."
                    console.apu.frameCounterTick(FrameType.HALF)
                    blockFrameCounterTick = 2
                }
            }
        }

        if (blockFrameCounterTick > 0) {
            blockFrameCounterTick--
        }

        return intArrayOf(cyclesRan, cyclesToRun)
    }

    fun needToRun(cycles: Int): Boolean {
        // Run APU when:
        // - A new value is pending
        // - The "blockFrameCounterTick" process is running
        // - We're at the before-last or last tick of the current step
        return newValue >= 0 ||
            blockFrameCounterTick > 0 ||
            (previousCycle + cycles) >= stepCycles[stepMode.toInt()][currentStep] - 1
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        ranges.addHandler(WRITE, 0x4017)
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        console.apu.run()

        newValue = value

        writeDelayCounter = if (console.cpu.cycleCount.bit0) {
            // If the write occurs between APU cycles, the effects occur 4 CPU
            // cycles after the write cycle.
            4
        } else {
            // If the write occurs during an APU cycle, the effects occur 3 CPU
            // cycles after the $4017 write cycle
            3
        }

        inhibitIRQ = value.bit6

        if (inhibitIRQ) {
            console.cpu.clearIRQSource(IRQSource.FRAME_COUNTER)
            irqFlag = false
            irqFlagClearClock = 0
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("previousCycle", previousCycle)
        s.write("irqFlag", irqFlag)
        s.write("irqFlagClearClock", irqFlagClearClock)
        s.write("currentStep", currentStep)
        s.write("stepMode", stepMode)
        s.write("inhibitIRQ", inhibitIRQ)
        s.write("blockFrameCounterTick", blockFrameCounterTick)
        s.write("writeDelayCounter", writeDelayCounter)
        s.write("newValue", newValue)
    }

    override fun restoreState(s: Snapshot) {
        previousCycle = s.readInt("previousCycle")
        irqFlag = s.readBoolean("irqFlag")
        irqFlagClearClock = s.readLong("irqFlagClearClock")
        currentStep = s.readInt("currentStep")
        stepMode = s.readBoolean("stepMode")
        inhibitIRQ = s.readBoolean("inhibitIRQ")
        blockFrameCounterTick = s.readInt("blockFrameCounterTick")
        writeDelayCounter = s.readInt("writeDelayCounter")
        newValue = s.readInt("newValue")
        updateRegion(console.region)
    }

    companion object {

        private val STEP_CYCLES_NTSC = arrayOf(
            intArrayOf(7457, 14913, 22371, 29828, 29829, 29830),
            intArrayOf(7457, 14913, 22371, 29829, 37281, 37282),
        )

        private val STEP_CYCLES_PAL = arrayOf(
            intArrayOf(8313, 16627, 24939, 33252, 33253, 33254),
            intArrayOf(8313, 16627, 24939, 33253, 41565, 41566),
        )

        private val FRAME_TYPE = arrayOf(
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
