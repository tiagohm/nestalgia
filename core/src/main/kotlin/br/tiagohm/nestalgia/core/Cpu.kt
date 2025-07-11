package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ConsoleType.HVC_001
import br.tiagohm.nestalgia.core.MemoryOperationType.*
import br.tiagohm.nestalgia.core.PSFlag.*
import br.tiagohm.nestalgia.core.Region.DENDY
import br.tiagohm.nestalgia.core.Region.PAL
import kotlin.random.Random

@Suppress("NOTHING_TO_INLINE")
class Cpu(private val console: Console) : Memory, Resetable, Initializable, Snapshotable {

    @Volatile private var startClockCount = 0
    @Volatile private var endClockCount = 0

    @JvmField @PublishedApi internal val state = CpuState()

    @JvmField internal var isCpuWrite = false
    @Volatile private var needHalt = false
    @Volatile private var abortDmcDma = false

    @Volatile private var needDummyRead = false

    @Volatile private var spriteDmaTransfer = false
    @Volatile private var dmcDmaRunning = false
    @Volatile private var spriteDmaOffset = 0

    @JvmField @PublishedApi internal var cycleCount = -1L
    @JvmField @PublishedApi internal var masterClock = 0L

    @Volatile private var ppuOffset = 0

    @Volatile private var prevRunIrq = false
    @Volatile private var runIrq = false

    @Volatile private var prevNmiFlag = false
    @Volatile private var prevNeedNmi = false
    @Volatile private var needNmi = false
    @Volatile private var irqMask = 0

    @Volatile private var addressMode = AddressMode.NONE
    @Volatile private var operand = 0

    private val cpuInstruction = CpuInstruction(this)

    inline var a
        get() = state.a
        private set(value) {
            state.a = register(value and 0xFF)
        }

    inline var x
        get() = state.x
        private set(value) {
            state.x = register(value and 0xFF)
        }

    inline var y
        get() = state.y
        private set(value) {
            state.y = register(value and 0xFF)
        }

    inline var ps
        get() = state.ps
        private set(value) {
            state.ps = value and 0xCF
        }

    inline var pc
        get() = state.pc
        private set(value) {
            state.pc = value and 0xFFFF
        }

    inline var sp
        get() = state.sp
        private set(value) {
            state.sp = value and 0xFF
        }

    private inline fun fetchOperandValue(): Int {
        return if (addressMode.ordinal >= AddressMode.ZERO.ordinal) read(operand)
        else operand and 0xFF
    }

    inline fun clearIRQSource(source: IRQSource) {
        state.irq = state.irq and source.code.inv()
    }

    inline fun setIRQSource(source: IRQSource) {
        state.irq = state.irq or source.code
    }

    inline fun hasIRQSource(source: IRQSource): Boolean {
        return (state.irq and source.code) != 0
    }

    private fun register(value: Int): Int {
        ZERO.clear()
        NEGATIVE.clear()
        zeroNegativeFlags(value)
        return value
    }

    private fun zeroNegativeFlags(value: Int) {
        if (value and 0xFF == 0) {
            ZERO.set()
        } else if (value.bit7) {
            NEGATIVE.set()
        }
    }

    internal fun aac() {
        a = a and fetchOperandValue()

        CARRY.clear()

        if (NEGATIVE.isOn()) {
            CARRY.set()
        }
    }

    internal fun adc() {
        add(fetchOperandValue())
    }

    private fun add(value: Int) {
        val sum = a + value + if (CARRY.isOn()) 0x01 else 0x00
        val result = sum and 0xFF

        CARRY.clear()
        NEGATIVE.clear()
        OVERFLOW.clear()
        ZERO.clear()

        zeroNegativeFlags(result)

        // if(~(A() ^ value) & (A() ^ result) & 0x80)
        if (((a xor value).inv() and (a xor result)).bit7) {
            OVERFLOW.set()
        }
        if (sum > 0xFF) {
            CARRY.set()
        }

        a = result
    }

    internal fun and() {
        a = a and fetchOperandValue()
    }

    internal fun arr() {
        a = (a and fetchOperandValue() shr 1) or if (CARRY.isOn()) 0x80 else 0x00

        CARRY.clear()
        OVERFLOW.clear()

        if (a.bit6) {
            CARRY.set()
        }

        if (CARRY.isOn() xor a.bit5) {
            OVERFLOW.set()
        }
    }

    private fun asl(value: Int): Int {
        CARRY.clear()
        NEGATIVE.clear()
        ZERO.clear()

        if (value.bit7) {
            CARRY.set()
        }

        val result = value shl 1
        zeroNegativeFlags(result)
        return result
    }

    internal fun aslAcc() {
        a = asl(a)
    }

    internal fun aslMem() {
        val addr = operand
        val value = read(addr)
        write(addr, value, DUMMY_WRITE) // Dummy write
        write(addr, asl(value))
    }

    internal fun asr() {
        CARRY.clear()

        a = a and fetchOperandValue()

        if (a.bit0) {
            CARRY.set()
        }

        a = a shr 1
    }

    internal fun atx() {
        a = fetchOperandValue() // LDA
        x = a // TAX
        a = a // Update flags based on A
    }

    internal fun axa() {
        val addr = operand
        // This opcode stores the result of A AND X AND the high byte of the
        // target address of the operand +1 in memory.
        // This may not be the actual behavior, but the read/write operations are
        // needed for proper cycle counting.
        write(addr, (addr.hiByte + 1) and a and x)
    }

    internal fun axs() {
        val value = fetchOperandValue()
        val ax = a and x and 0xFF
        val result = ax - value

        CARRY.clear()

        if (ax >= value) {
            CARRY.set()
        }

        x = result
    }

    private fun branchRelative(branch: Boolean) {
        if (branch) {
            val offset = operand.toByte()

            // A taken non-page-crossing branch ignores IRQ/NMI during its last clock,
            // so that next instruction executes before the IRQ
            // Fixes "branch_delays_irq" test
            if (runIrq && !prevRunIrq) {
                runIrq = false
            }

            dummyRead()

            if (isSignedPageCrossed(pc, offset)) {
                dummyRead()
            }

            pc += offset
        }
    }

    internal fun bcc() {
        branchRelative(CARRY.isOff())
    }

    internal fun bcs() {
        branchRelative(CARRY.isOn())
    }

    internal fun beq() {
        branchRelative(ZERO.isOn())
    }

    internal fun bit() {
        val value = fetchOperandValue()

        ZERO.clear()
        OVERFLOW.clear()
        NEGATIVE.clear()

        if ((a and value) == 0) {
            ZERO.set()
        }
        if (value.bit6) {
            OVERFLOW.set()
        }
        if (value.bit7) {
            NEGATIVE.set()
        }
    }

    internal fun bmi() {
        branchRelative(NEGATIVE.isOn())
    }

    internal fun bne() {
        branchRelative(ZERO.isOff())
    }

    internal fun bpl() {
        branchRelative(NEGATIVE.isOff())
    }

    internal fun brk() {
        push16(pc + 1)

        val flags = ps or BREAK.code or RESERVED.code

        if (needNmi) {
            needNmi = false

            push8(flags)
            INTERRUPT.set()

            pc = readWord(NMI_VECTOR)
        } else {
            push8(flags)
            INTERRUPT.set()

            pc = readWord(IRQ_VECTOR)
        }

        // Ensure we don't start an NMI right after running a BRK instruction
        // (first instruction in IRQ handler must run first - needed for nmi_and_brk test).
        prevNeedNmi = false
    }

    internal fun bvc() {
        branchRelative(OVERFLOW.isOff())
    }

    internal fun bvs() {
        branchRelative(OVERFLOW.isOn())
    }

    internal fun clc() {
        CARRY.clear()
    }

    internal fun cld() {
        DECIMAL.clear()
    }

    internal fun cli() {
        INTERRUPT.clear()
    }

    internal fun clv() {
        OVERFLOW.clear()
    }

    internal fun cpa() {
        cmp(a, fetchOperandValue())
    }

    internal fun cpx() {
        cmp(x, fetchOperandValue())
    }

    internal fun cpy() {
        cmp(y, fetchOperandValue())
    }

    internal fun dcp() {
        var value = fetchOperandValue()
        write(operand, value, DUMMY_WRITE) // Dummy write
        value--
        cmp(a, value and 0xFF)
        write(operand, value)
    }

    internal fun dec() {
        val addr = operand

        NEGATIVE.clear()
        ZERO.clear()

        var value = read(addr)

        write(addr, value, DUMMY_WRITE) // Dummy write

        value--

        zeroNegativeFlags(value)
        write(addr, value)
    }

    internal fun dex() {
        x--
    }

    internal fun dey() {
        y--
    }

    internal fun eor() {
        a = a xor fetchOperandValue()
    }

    internal fun hlt() {
        // Normally freezes the cpu, we can probably assume nothing will ever call this.
        fetchOperandValue()
    }

    internal fun inc() {
        val addr = operand

        NEGATIVE.clear()
        ZERO.clear()

        var value = read(addr)

        write(addr, value, DUMMY_WRITE) // Dummy write

        value++

        zeroNegativeFlags(value)
        write(addr, value)
    }

    internal fun inx() {
        x++
    }

    internal fun iny() {
        y++
    }

    internal fun isb() {
        var value = fetchOperandValue()
        write(operand, value, DUMMY_WRITE) // Dummy write
        value++
        add(value and 0xFF xor 0xFF) // TODO: VERIFICAR AND 0xFF ESTÁ CORRETO
        write(operand, value)
    }

    internal fun jmp(addr: Int) {
        pc = addr
    }

    internal fun jmpAbs() {
        jmp(operand)
    }

    internal fun jmpInd() {
        jmp(readInd())
    }

    internal fun jsr() {
        val addr = operand
        dummyRead()
        push16(pc - 1)
        jmp(addr)
    }

    internal fun las() {
        // AND memory with stack pointer, transfer result to accumulator,
        // X register and stack pointer.
        val value = fetchOperandValue()
        a = value and sp
        x = a
        sp = a
    }

    internal fun lax() {
        val value = fetchOperandValue()
        x = value
        a = value
    }

    internal fun lda() {
        a = fetchOperandValue()
    }

    internal fun ldx() {
        x = fetchOperandValue()
    }

    internal fun ldy() {
        y = fetchOperandValue()
    }

    private fun lsr(value: Int): Int {
        CARRY.clear()
        NEGATIVE.clear()
        ZERO.clear()

        if (value.bit0) {
            CARRY.set()
        }

        val result = value shr 1
        zeroNegativeFlags(result)
        return result
    }

    internal fun lsrAcc() {
        a = lsr(a)
    }

    internal fun lsrMem() {
        val addr = operand
        val value = read(addr)
        write(addr, value, DUMMY_WRITE) // Dummy write
        write(addr, lsr(value))
    }

    internal fun nop() {
        // Make sure the nop operation takes as many cycles as meant to
        fetchOperandValue()
    }

    internal fun ora() {
        a = a or fetchOperandValue()
    }

    internal fun pha() {
        push8(a)
    }

    internal fun php() {
        push8(ps or BREAK.code or RESERVED.code)
    }

    internal fun pla() {
        dummyRead()
        a = pop8()
    }

    internal fun plp() {
        dummyRead()
        ps = pop8()
    }

    internal fun rla() {
        val value = fetchOperandValue()
        write(operand, value, DUMMY_WRITE) // Dummy write
        val shiftedValue = rol(value)
        a = a and shiftedValue
        write(operand, shiftedValue)
    }

    private fun rol(value: Int): Int {
        val isCarry = CARRY.isOn()

        CARRY.clear()
        NEGATIVE.clear()
        ZERO.clear()

        if (value.bit7) {
            CARRY.set()
        }

        val result = (value shl 1) or if (isCarry) 0x01 else 0x00
        zeroNegativeFlags(result)
        return result
    }

    internal fun rolAcc() {
        a = rol(a)
    }

    internal fun rolMem() {
        val addr = operand
        val value = read(addr)
        write(addr, value, DUMMY_WRITE) // Dummy write
        write(addr, rol(value))
    }

    private fun ror(value: Int): Int {
        val carry = CARRY.isOn()

        CARRY.clear()
        NEGATIVE.clear()
        ZERO.clear()

        if (value.bit0) {
            CARRY.set()
        }

        val result = (value shr 1) or if (carry) 0x80 else 0x00
        zeroNegativeFlags(result)
        return result
    }

    internal fun rorAcc() {
        a = ror(a)
    }

    internal fun rorMem() {
        val addr = operand
        val value = read(addr)
        write(addr, value, DUMMY_WRITE) // Dummy write
        write(addr, ror(value))
    }

    internal fun rra() {
        val value = fetchOperandValue()
        write(operand, value, DUMMY_WRITE) // Dummy write
        val shiftedValue = ror(value)
        add(shiftedValue)
        write(operand, shiftedValue)
    }

    internal fun rti() {
        dummyRead()
        ps = pop8()
        pc = pop16()
    }

    internal fun rts() {
        val addr = pop16()
        dummyRead()
        dummyRead()
        pc = addr + 1
    }

    internal fun sax() {
        write(operand, a and x)
    }

    internal fun sbc() {
        add(fetchOperandValue() xor 0xFF)
    }

    internal fun sec() {
        CARRY.set()
    }

    internal fun sed() {
        DECIMAL.set()
    }

    internal fun sei() {
        INTERRUPT.set()
    }

    internal fun slo() {
        val value = fetchOperandValue()
        write(operand, value, DUMMY_WRITE) // Dummy write
        val shiftedValue = asl(value)
        a = a or shiftedValue
        write(operand, shiftedValue)
    }

    internal fun sre() {
        val value = fetchOperandValue()
        write(operand, value, DUMMY_WRITE) // Dummy write
        val shiftedValue = lsr(value)
        a = a xor shiftedValue
        write(operand, shiftedValue)
    }

    internal fun sta() {
        write(operand, a)
    }

    internal fun stx() {
        write(operand, x)
    }

    internal fun sty() {
        write(operand, y)
    }

    internal fun sxa() {
        val hi = operand.hiByte
        val lo = operand.loByte
        val value = x and (hi + 1)

        write(((x and (hi + 1)) shl 8) or lo, value)
    }

    internal fun sya() {
        val hi = operand.hiByte
        val lo = operand.loByte
        val value = y and (hi + 1)

        // From here: http://forums.nesdev.com/viewtopic.php?f=3&t=3831&start=30
        // Unsure if this is accurate or not
        // the target address for e.g. SYA becomes ((y & (addr_high + 1)) << 8) | addr_low instead of the normal ((addr_high + 1) << 8) | addr_low
        write(((y and (hi + 1)) shl 8) or lo, value)
    }

    internal fun tas() {
        // AND X register with accumulator and store result in stack
        // pointer, then AND stack pointer with the high byte of the
        // target address of the argument + 1. Store result in memory.
        val addr = operand
        sp = x and a
        write(addr, sp and (addr.hiByte + 1))
    }

    internal fun tax() {
        x = a
    }

    internal fun tay() {
        y = a
    }

    internal fun tsx() {
        x = sp
    }

    internal fun txa() {
        a = x
    }

    internal fun txs() {
        sp = x
    }

    internal fun tya() {
        a = y
    }

    internal fun unk() {
        // Make sure we take the right amount of cycles
        // (not reliable for operations that write to memory, etc).
        fetchOperandValue()
    }

    private fun cmp(reg: Int, value: Int) {
        CARRY.clear()
        NEGATIVE.clear()
        ZERO.clear()

        val result = reg - value

        if (reg >= value) {
            CARRY.set()
        }
        if (reg == value) {
            ZERO.set()
        }
        if (result.bit7) {
            NEGATIVE.set()
        }
    }

    override fun initialize() {
        masterClockDivider(console.region)
    }

    override fun reset(softReset: Boolean) {
        state.nmi = false
        state.irq = 0

        spriteDmaTransfer = false
        spriteDmaOffset = 0
        needHalt = false
        dmcDmaRunning = false
        abortDmcDma = false

        // Use _memoryManager->Read() directly to prevent clocking the PPU/APU
        // when setting PC at reset.
        state.pc = console.memoryManager.readWord(RESET_VECTOR)

        if (softReset) {
            INTERRUPT.set()
            state.sp = (state.sp - 0x03) and 0xFF
        } else {
            //Used by NSF code to disable Frame Counter & DMC interrupts
            irqMask = 0xFF

            state.a = 0
            state.sp = 0xFD
            state.x = 0
            state.y = 0
            state.ps = INTERRUPT.code

            runIrq = false
        }

        val ppuDivider: Int
        val cpuDivider: Int

        when (console.region) {
            PAL -> {
                ppuDivider = 5
                cpuDivider = 16
            }
            DENDY -> {
                ppuDivider = 5
                cpuDivider = 15
            }
            else -> {
                ppuDivider = 4
                cpuDivider = 12
            }
        }

        cycleCount = -1L
        masterClock = 0

        val cpuOffset: Int

        if (console.settings.flag(EmulationFlag.RANDOMIZE_CPU_PPU_ALIGNMENT)) {
            ppuOffset = Random.nextInt(ppuDivider)
            cpuOffset = Random.nextInt(cpuDivider)
        } else {
            ppuOffset = 1
            cpuOffset = 0
        }

        masterClock += cpuDivider + cpuOffset

        // The CPU takes 8 cycles before it starts executing the ROM's
        // code after a reset/power up.
        repeat(8) {
            startCpuCycle(true)
            endCpuCycle(true)
        }
    }

    private inline fun readOpcode(): Int {
        return read(state.pc++, OPCODE)
    }

    private inline fun dummyRead(): Int {
        read(state.pc, DUMMY_READ)
        return 0
    }

    private inline fun readByte(): Int {
        return read(state.pc++, OPERAND)
    }

    private inline fun readWord(): Int {
        val lo = readByte()
        val hi = readByte()
        return (hi shl 8) or lo
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        processPendingDma(addr)
        startCpuCycle(true)
        val value = console.memoryManager.read(addr, type)
        endCpuCycle(true)
        return value
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        isCpuWrite = true
        startCpuCycle(false)
        console.memoryManager.write(addr, value, type)
        endCpuCycle(false)
        isCpuWrite = false
    }

    private inline fun push8(value: Int) {
        write(sp + 0x100, value)
        sp--
    }

    private inline fun push16(value: Int) {
        push8(value.hiByte)
        push8(value.loByte)
    }

    private inline fun pop8(): Int {
        sp++
        return read(0x100 + sp)
    }

    private inline fun pop16(): Int {
        val lo = pop8()
        val hi = pop8()
        return lo or (hi shl 8)
    }

    private inline fun processCycle() {
        // Sprite DMA cycles count as halt/dummy cycles for the DMC DMA when both
        // run at the same time.
        if (abortDmcDma) {
            dmcDmaRunning = false
            abortDmcDma = false
            needDummyRead = false
            needHalt = false
        } else if (needHalt) {
            needHalt = false
        } else if (needDummyRead) {
            needDummyRead = false
        }

        startCpuCycle(true)
    }

    private fun processPendingDma(addr: Int) {
        if (!needHalt) return

        val prevReadAddress = intArrayOf(addr)
        val enableInternalRegReads = (addr and 0xFFE0) == 0x4000
        var skipFirstInputClock = false

        if (enableInternalRegReads && dmcDmaRunning && (addr == 0x4016 || addr == 0x4017)) {
            val dmcAddress = console.apu.dmcReadAddress

            if (dmcAddress and 0x1F == addr and 0x1F) {
                //DMC will cause a read on the same address as the CPU was reading from
                //This will hide the reads from the controllers because /OE will be active the whole time
                skipFirstInputClock = true
            }
        }

        // On PAL, the dummy/idle reads done by the DMA don't appear to be done on the
        // address that the CPU was about to read. This prevents the 2+x reads on registers issues.
        // The exact specifics of where the CPU reads instead aren't known yet - so just disable read side-effects entirely on PAL
        val isNtscInputBehavior = console.region != PAL

        // If this cycle is a read, hijack the read, discard the value, and prevent all other actions that occur on this cycle (PC not incremented, etc)
        startCpuCycle(true)

        if (abortDmcDma && (addr == 0x4016 || addr == 0x4017)) {
            // Skip halt cycle dummy read on 4016/4017
            // The DMA was aborted, and the CPU will read 4016/4017 next
            // If 4016/4017 is read here, the controllers will see 2 separate reads
            // even though they would only see a single read on hardware (except the original Famicom)
        } else if (isNtscInputBehavior && !skipFirstInputClock) {
            console.memoryManager.read(addr, DUMMY_READ)
        }

        endCpuCycle(true)

        if (abortDmcDma) {
            dmcDmaRunning = false
            abortDmcDma = false

            if (!spriteDmaTransfer) {
                // If DMC DMA was cancelled and OAM DMA isn't about to start,
                // stop processing DMA entirely. Otherwise, OAM DMA needs to run,
                // so the DMA process has to continue.
                needDummyRead = false
                return
            }
        }

        needHalt = false

        var spriteDmaCounter = 0
        var spriteReadAddr = 0
        var readValue = 0

        // On Famicom, each dummy/idle read to 4016/4017 is intepreted as a read of the joypad registers
        // On NES (or AV Famicom), only the first dummy/idle read causes side effects (e.g only a single bit is lost)
        val type = console.settings.consoleType
        val isNesBehavior = type != HVC_001
        val skipDummyReads = !isNtscInputBehavior || (isNesBehavior && (addr == 0x4016 || addr == 0x4017))

        while (dmcDmaRunning || spriteDmaTransfer) {
            val cycle = !cycleCount.bit0

            if (cycle) {
                if (dmcDmaRunning && !needHalt && !needDummyRead) {
                    // DMC DMA is ready to read a byte (both halt and dummy read
                    // cycles were performed before this).
                    processCycle()
                    readValue = processDmaRead(console.apu.dmcReadAddress, prevReadAddress, enableInternalRegReads, isNesBehavior)
                    endCpuCycle(true)
                    console.apu.dmcReadBuffer(readValue)
                    dmcDmaRunning = false
                    abortDmcDma = false
                } else if (spriteDmaTransfer) {
                    // DMC DMA is not running, or not ready, run sprite DMA.
                    processCycle()
                    readValue = processDmaRead(spriteDmaOffset * 0x100 + spriteReadAddr, prevReadAddress, enableInternalRegReads, isNesBehavior)
                    endCpuCycle(true)
                    spriteReadAddr++
                    spriteDmaCounter++
                } else {
                    // DMC DMA is running, but not ready (need halt/dummy read)
                    // and sprite DMA isn't runnnig, perform a dummy read.
                    // assert(isNeedHalt || isNeedDummyRead)
                    processCycle()

                    if (!skipDummyReads) {
                        processDmaRead(spriteDmaOffset * 0x100 + spriteReadAddr, prevReadAddress, enableInternalRegReads, isNesBehavior)
                    }

                    endCpuCycle(true)
                }
            } else {
                if (spriteDmaTransfer && spriteDmaCounter.bit0) {
                    //Sprite DMA write cycle (only do this if a sprite dma read
                    // was performed last cycle).
                    processCycle()
                    console.memoryManager.write(0x2004, readValue, DMA_WRITE)
                    endCpuCycle(true)
                    spriteDmaCounter++

                    if (spriteDmaCounter == 0x200) {
                        spriteDmaTransfer = false
                    }
                } else {
                    // Align to read cycle before starting sprite DMA
                    // (or align to perform DMC read).
                    processCycle()

                    if (!skipDummyReads) {
                        processDmaRead(spriteDmaOffset * 0x100 + spriteReadAddr, prevReadAddress, enableInternalRegReads, isNesBehavior)
                    }

                    endCpuCycle(true)
                }
            }
        }
    }

    private fun processDmaRead(addr: Int, prevReadAddress: IntArray, enableInternalRegReads: Boolean, isNesBehavior: Boolean): Int {
        // This is to reproduce a CPU bug that can occur during DMA which can cause the 2A03 to read from
        // its internal registers (4015, 4016, 4017) at the same time as the DMA unit reads a byte from
        // the bus. This bug occurs if the CPU is halted while it's reading a value in the $4000-$401F range.
        //
        // This has a number of side effects:
        // -It can cause a read of $4015 to occur without the program's knowledge, which would clear the frame counter's IRQ flag
        // -It can cause additional bit deletions while reading the input (e.g more than the DMC glitch usually causes)
        // -It can also *prevent* bit deletions from occurring at all in another scenario
        // -It can replace/corrupt the byte that the DMA is reading, causing DMC to play the wrong sample

        var value: Int

        if (!enableInternalRegReads) {
            value = if (addr in 0x4000..0x401F) {
                // Nothing will respond on $4000-$401F on the external bus - return open bus value
                console.memoryManager.openBus()
            } else {
                console.memoryManager.read(addr, DMA_READ)
            }

            prevReadAddress[0] = addr

            return value
        } else {
            // This glitch causes the CPU to read from the internal APU/Input registers
            // regardless of the address the DMA unit is trying to read
            val internalAddr = 0x4000 or (addr and 0x1F)
            val isSameAddress = internalAddr == addr

            when (internalAddr) {
                0x4015 -> {
                    value = console.memoryManager.read(internalAddr, DMA_READ)

                    if (!isSameAddress) {
                        // Also trigger a read from the actual address the CPU was supposed to read from (external bus)
                        console.memoryManager.read(addr, DMA_READ)
                    }
                }
                0x4016,
                0x4017 -> {
                    value = if (console.region == PAL || (isNesBehavior && prevReadAddress[0] == internalAddr)) {
                        // Reading from the same input register twice in a row, skip the read entirely to avoid
                        // triggering a bit loss from the read, since the controller won't react to this read
                        // Return the same value as the last read, instead
                        // On PAL, the behavior is unknown - for now, don't cause any bit deletions
                        console.memoryManager.openBus()
                    } else {
                        console.memoryManager.read(internalAddr, DMA_READ)
                    }

                    if (!isSameAddress) {
                        // The DMA unit is reading from a different address, read from it too (external bus)
                        val obMask = console.controlManager.openBusMask(internalAddr - 0x4016)
                        val externalValue = console.memoryManager.read(addr, DMA_READ)

                        // Merge values, keep the external value for all open bus pins on the 4016/4017 port
                        // AND all other bits together (bus conflict)
                        value = (externalValue and obMask) or ((value and obMask.inv()) and (externalValue and obMask.inv()))
                    }
                }
                else -> {
                    value = console.memoryManager.read(addr, DMA_READ)
                }
            }

            prevReadAddress[0] = internalAddr

            return value
        }
    }

    fun stopDmcTransfer() {
        if (dmcDmaRunning) {
            if (needHalt) {
                // If interrupted before the halt cycle starts, cancel DMA completely
                // This can happen when a write prevents the DMA from starting after being queued
                dmcDmaRunning = false
                needDummyRead = false
                needHalt = false
            } else {
                // Abort DMA if possible (this only appears to be possible if done within the first cycle of DMA)
                abortDmcDma = true
            }
        }
    }

    private fun startCpuCycle(forRead: Boolean) {
        masterClock += if (forRead) startClockCount - 1 else startClockCount + 1
        cycleCount++
        console.ppu.run(masterClock - ppuOffset)
        console.processCpuClock()
    }

    private fun endCpuCycle(forRead: Boolean) {
        masterClock += if (forRead) endClockCount + 1 else endClockCount - 1
        console.ppu.run(masterClock - ppuOffset)

        // The internal signal goes high during φ1 of the cycle that follows the one where the edge is detected,
        // and stays high until the NMI has been handled.
        prevNeedNmi = needNmi

        // This edge detector polls the status of the NMI line during φ2 of each CPU cycle (i.e., during the
        // second half of each cycle) and raises an internal signal if the input goes from being high during
        // one cycle to being low during the next
        if (!prevNmiFlag && state.nmi) {
            needNmi = true
        }

        prevNmiFlag = state.nmi

        // it's really the status of the interrupt lines at the end of the second-to-last cycle that matters.
        // Keep the irq lines values from the previous cycle.  The before-to-last cycle's values will be used
        prevRunIrq = runIrq
        runIrq = (state.irq and irqMask) > 0 && INTERRUPT.isOff()
    }

    private inline fun PSFlag.isOn(): Boolean {
        return state.ps and code == code
    }

    private inline fun PSFlag.isOff(): Boolean {
        return state.ps and code != code
    }

    private inline fun PSFlag.clear() {
        state.ps = state.ps and code.inv()
    }

    private inline fun PSFlag.set() {
        state.ps = state.ps or code
    }

    fun runDmaTransfer(offset: Int) {
        spriteDmaTransfer = true
        spriteDmaOffset = offset
        needHalt = true
    }

    fun startDmcTransfer() {
        dmcDmaRunning = true
        needDummyRead = true
        needHalt = true
    }

    fun exec() {
        val opcode = readOpcode()
        addressMode = ADDRESS_MODES[opcode]
        operand = fetchOperand()

        cpuInstruction.execute(opcode)

        if (prevRunIrq || prevNeedNmi) {
            irq()
        }
    }

    private fun fetchOperand(): Int {
        when (addressMode) {
            AddressMode.ACC,
            AddressMode.IMP -> return dummyRead()
            AddressMode.IMM,
            AddressMode.REL -> return readImmediate()
            AddressMode.ZERO -> return readZeroAddr()
            AddressMode.ZERO_X -> return readZeroXAddr()
            AddressMode.ZERO_Y -> return readZeroYAddr()
            AddressMode.IND -> return readIndAddr()
            AddressMode.IND_X -> return readIndXAddr()
            AddressMode.IND_Y -> return readIndYAddr(false)
            AddressMode.IND_YW -> return readIndYAddr(true)
            AddressMode.ABS -> return readAbsAddr()
            AddressMode.ABS_X -> return readAbsXAddr(false)
            AddressMode.ABS_XW -> return readAbsXAddr(true)
            AddressMode.ABS_Y -> return readAbsYAddr(false)
            AddressMode.ABS_YW -> return readAbsYAddr(true)
            AddressMode.NONE -> Unit
        }

        if (console.isNsf) {
            // Don't stop emulation on CPU crash when playing NSFs, reset cpu instead
            console.reset(true)
        }

        return 0
    }

    private fun irq() {
        // Fetch opcode (and discard it - $00 (BRK) is forced into the opcode register instead)
        dummyRead()
        // Read next instruction byte (actually the same as above,
        // since PC increment is suppressed. Also discarded).
        dummyRead()

        push16(pc)

        if (needNmi) {
            needNmi = false

            push8(ps or RESERVED.code)
            INTERRUPT.set()

            pc = readWord(NMI_VECTOR)
        } else {
            push8(ps or RESERVED.code)
            INTERRUPT.set()

            pc = readWord(IRQ_VECTOR)
        }
    }

    private inline fun readImmediate() = readByte()

    private inline fun readIndAddr() = readWord()

    private inline fun readZeroAddr() = readByte()

    private fun readZeroXAddr(): Int {
        val value = readByte()
        read(value, DUMMY_READ)
        return (value + x) and 0xFF
    }

    private fun readZeroYAddr(): Int {
        val value = readByte()
        read(value, DUMMY_READ)
        return (value + y) and 0xFF
    }

    private inline fun readAbsAddr() = readWord()

    private fun readAbsXAddr(dummyRead: Boolean): Int {
        val base = readWord()
        val pageCrossed = isPageCrossed(base, x)

        if (pageCrossed || dummyRead) {
            // Dummy read done by the processor (only when page is crossed for READ instructions)
            read(base + x - if (pageCrossed) 0x100 else 0, DUMMY_READ)
        }

        return (base + x) and 0xFFFF
    }

    private fun readAbsYAddr(dummyRead: Boolean): Int {
        val base = readWord()
        val pageCrossed = isPageCrossed(base, y)

        if (pageCrossed || dummyRead) {
            // Dummy read done by the processor (only when page is crossed for READ instructions)
            read(base + y - if (pageCrossed) 0x100 else 0, DUMMY_READ)
        }

        return (base + y) and 0xFFFF
    }

    private fun readInd(): Int {
        val addr = operand

        return if (addr and 0xFF == 0xFF) {
            val lo = read(addr)
            val hi = read(addr - 0xFF)
            lo or (hi shl 8)
        } else {
            readWord(addr)
        }
    }

    private fun readIndXAddr(): Int {
        var zero = readByte()

        // Dummy read
        read(zero, DUMMY_READ)

        zero = (zero + x) and 0xFF

        return if (zero == 0xFF) {
            val lo = read(0xFF)
            val hi = read(0x00)
            lo or (hi shl 8)
        } else {
            readWord(zero)
        }
    }

    private fun readIndYAddr(dummyRead: Boolean): Int {
        val zero = readByte()

        val addr = if (zero == 0xFF) {
            val lo = read(0xFF)
            val hi = read(0x00)
            lo or (hi shl 8)
        } else {
            readWord(zero)
        }

        val pageCrossed = isPageCrossed(addr, y)

        if (pageCrossed || dummyRead) {
            // Dummy read done by the processor (only when page is crossed for READ instructions)
            read(addr + y - if (pageCrossed) 0x100 else 0, DUMMY_READ)
        }

        return (addr + y) and 0xFFFF
    }

    private inline fun isSignedPageCrossed(a: Int, b: Byte): Boolean {
        return ((a + b) and 0xFF00) != (a and 0xFF00)
    }

    private inline fun isPageCrossed(a: Int, b: Int): Boolean {
        return ((a + b) and 0xFF00) != (a and 0xFF00)
    }

    fun masterClockDivider(region: Region) {
        when (region) {
            PAL -> {
                startClockCount = 8
                endClockCount = 8
            }
            DENDY -> {
                startClockCount = 7
                endClockCount = 8
            }
            else -> {
                startClockCount = 6
                endClockCount = 6
            }
        }
    }

    inline var nmi
        get() = state.nmi
        set(value) {
            state.nmi = value
        }

    override fun saveState(s: Snapshot) {
        val settings = console.settings
        val extraScanlinesBeforeNmi = settings.extraScanlinesBeforeNmi
        val extraScanlinesAfterNmi = settings.extraScanlinesAfterNmi
        val dipSwitches = settings.dipSwitches

        s.write("state", state)
        s.write("cycleCount", cycleCount)
        s.write("dmcDmaRunning", dmcDmaRunning)
        s.write("spriteDmaTransfer", spriteDmaTransfer)
        s.write("extraScanlinesBeforeNmi", extraScanlinesBeforeNmi)
        s.write("extraScanlinesAfterNmi", extraScanlinesAfterNmi)
        s.write("dipSwitches", dipSwitches)
        s.write("needDummyRead", needDummyRead)
        s.write("needHalt", needHalt)
        s.write("startClockCount", startClockCount)
        s.write("endClockCount", endClockCount)
        s.write("ppuOffset", ppuOffset)
        s.write("masterClock", masterClock)
        s.write("prevNeedNmi", prevNeedNmi)
        s.write("prevNmiFlag", prevNmiFlag)
        s.write("needNmi", needNmi)
        s.write("abortDmcDma", abortDmcDma)
    }

    override fun restoreState(s: Snapshot) {
        s.readSnapshotable("state", state)
        cycleCount = s.readLong("cycleCount", -1L)
        dmcDmaRunning = s.readBoolean("dmcDmaRunning")
        spriteDmaTransfer = s.readBoolean("spriteDmaTransfer")
        val extraScanlinesBeforeNmi = s.readInt("extraScanlinesBeforeNmi")
        val extraScanlinesAfterNmi = s.readInt("extraScanlinesAfterNmi")
        val dipSwitches = s.readInt("dipSwitches")
        needDummyRead = s.readBoolean("needDummyRead")
        needHalt = s.readBoolean("needHalt")
        startClockCount = s.readInt("startClockCount")
        endClockCount = s.readInt("endClockCount")
        ppuOffset = s.readInt("ppuOffset")
        masterClock = s.readLong("masterClock")
        prevNeedNmi = s.readBoolean("prevNeedNmi")
        prevNmiFlag = s.readBoolean("prevNmiFlag")
        needNmi = s.readBoolean("needNmi")
        abortDmcDma = s.readBoolean("abortDmcDma")

        console.settings.extraScanlinesAfterNmi = extraScanlinesAfterNmi
        console.settings.extraScanlinesBeforeNmi = extraScanlinesBeforeNmi
        console.settings.dipSwitches = dipSwitches
    }

    companion object {

        const val NMI_VECTOR = 0xFFFA
        const val RESET_VECTOR = 0xFFFC
        const val IRQ_VECTOR = 0xFFFE
        const val CLOCK_RATE_NTSC = 1789773
        const val CLOCK_RATE_PAL = 1662607
        const val CLOCK_RATE_DENDY = 1773448

        private val ADDRESS_MODES = arrayOf(
            AddressMode.IMP, AddressMode.IND_X, AddressMode.NONE, AddressMode.IND_X,
            AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO,
            AddressMode.IMP, AddressMode.IMM, AddressMode.ACC, AddressMode.IMM,
            AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW,
            AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X,
            AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,
            AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.ABS, AddressMode.IND_X, AddressMode.NONE, AddressMode.IND_X,
            AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO,
            AddressMode.IMP, AddressMode.IMM, AddressMode.ACC, AddressMode.IMM,
            AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW,
            AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X,
            AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,
            AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.IMP, AddressMode.IND_X, AddressMode.NONE, AddressMode.IND_X,
            AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO,
            AddressMode.IMP, AddressMode.IMM, AddressMode.ACC, AddressMode.IMM,
            AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW,
            AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X,
            AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,
            AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.IMP, AddressMode.IND_X, AddressMode.NONE, AddressMode.IND_X,
            AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO,
            AddressMode.IMP, AddressMode.IMM, AddressMode.ACC, AddressMode.IMM,
            AddressMode.IND, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW,
            AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X,
            AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,
            AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.IMM, AddressMode.IND_X, AddressMode.IMM, AddressMode.IND_X,
            AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO,
            AddressMode.IMP, AddressMode.IMM, AddressMode.IMP, AddressMode.IMM,
            AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_YW, AddressMode.NONE, AddressMode.IND_YW,
            AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_Y, AddressMode.ZERO_Y,
            AddressMode.IMP, AddressMode.ABS_YW, AddressMode.IMP, AddressMode.ABS_YW,
            AddressMode.ABS_XW, AddressMode.ABS_XW, AddressMode.ABS_YW, AddressMode.ABS_YW,
            AddressMode.IMM, AddressMode.IND_X, AddressMode.IMM, AddressMode.IND_X,
            AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO,
            AddressMode.IMP, AddressMode.IMM, AddressMode.IMP, AddressMode.IMM,
            AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_Y,
            AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_Y, AddressMode.ZERO_Y,
            AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_Y,
            AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_Y, AddressMode.ABS_Y,
            AddressMode.IMM, AddressMode.IND_X, AddressMode.IMM, AddressMode.IND_X,
            AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO,
            AddressMode.IMP, AddressMode.IMM, AddressMode.IMP, AddressMode.IMM,
            AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW,
            AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X,
            AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,
            AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.IMM, AddressMode.IND_X, AddressMode.IMM, AddressMode.IND_X,
            AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO,
            AddressMode.IMP, AddressMode.IMM, AddressMode.IMP, AddressMode.IMM,
            AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW,
            AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X,
            AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,
            AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
        )
    }
}
