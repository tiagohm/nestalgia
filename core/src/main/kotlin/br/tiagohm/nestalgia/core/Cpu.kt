package br.tiagohm.nestalgia.core

import kotlin.random.Random

@ExperimentalUnsignedTypes
@Suppress("NOTHING_TO_INLINE")
class Cpu(val console: Console) :
    Memory,
    Snapshotable {

    private var startClockCount: UByte = 0U
    private var endClockCount: UByte = 0U

    val state = CpuState()

    private val memoryManager = console.memoryManager

    var isCpuWrite = false
        private set
    var isNeedHalt = false
        private set
    var isNeedDummyRead = false
        private set
    var spriteDmaTransfer = false
        private set
    var dmcDmaRunning = false
        private set
    var spriteDmaOffset: UByte = 0U
        private set

    var cycleCount = -1L
        private set
    var masterClock = 0L
        private set

    var ppuOffset: UByte = 0U
        private set

    var prevRunIrq = false
        private set
    var runIrq = false
        private set

    var prevNmiFlag = false
        private set
    var prevIsNeedNmi = false
        private set
    var isNeedNmi = false
        private set

    var irqMask: UByte = 0U

    var addressMode = AddressMode.NONE
        private set
    var operand: UShort = 0U
        private set

    private val opTable: Array<() -> Unit>

    init {
        // @formatter:off
        opTable = arrayOf(
            ::brk, ::ora, ::hlt, ::slo, ::nop, ::ora, ::aslMem, ::slo, ::php, ::ora, ::aslAcc, ::aac, ::nop, ::ora, ::aslMem, ::slo,
            ::bpl, ::ora, ::hlt, ::slo, ::nop, ::ora, ::aslMem, ::slo, ::clc, ::ora, ::nop, ::slo, ::nop, ::ora, ::aslMem, ::slo,
            ::jsr, ::and, ::hlt, ::rla, ::bit, ::and, ::rolMem, ::rla, ::plp, ::and, ::rolAcc, ::aac, ::bit, ::and, ::rolMem, ::rla,
            ::bmi, ::and, ::hlt, ::rla, ::nop, ::and, ::rolMem, ::rla, ::sec, ::and, ::nop, ::rla, ::nop, ::and, ::rolMem, ::rla,
            ::rti, ::eor, ::hlt, ::sre, ::nop, ::eor, ::lsrMem, ::sre, ::pha, ::eor, ::lsrAcc, ::asr, ::jmpAbs, ::eor, ::lsrMem, ::sre,
            ::bvc, ::eor, ::hlt, ::sre, ::nop, ::eor, ::lsrMem, ::sre, ::cli, ::eor, ::nop, ::sre, ::nop, ::eor, ::lsrMem, ::sre,
            ::rts, ::adc, ::hlt, ::rra, ::nop, ::adc, ::rorMem, ::rra, ::pla, ::adc, ::rorAcc, ::arr, ::jmpInd, ::adc, ::rorMem, ::rra,
            ::bvs, ::adc, ::hlt, ::rra, ::nop, ::adc, ::rorMem, ::rra, ::sei, ::adc, ::nop, ::rra, ::nop, ::adc, ::rorMem, ::rra,
            ::nop, ::sta, ::nop, ::sax, ::sty, ::sta, ::stx, ::sax, ::dey, ::nop, ::txa, ::unk, ::sty, ::sta, ::stx, ::sax,
            ::bcc, ::sta, ::hlt, ::axa, ::sty, ::sta, ::stx, ::sax, ::tya, ::sta, ::txs, ::tas, ::sya, ::sta, ::sxa, ::axa,
            ::ldy, ::lda, ::ldx, ::lax, ::ldy, ::lda, ::ldx, ::lax, ::tay, ::lda, ::tax, ::atx, ::ldy, ::lda, ::ldx, ::lax,
            ::bcs, ::lda, ::hlt, ::lax, ::ldy, ::lda, ::ldx, ::lax, ::clv, ::lda, ::tsx, ::las, ::ldy, ::lda, ::ldx, ::lax,
            ::cpy, ::cpa, ::nop, ::dcp, ::cpy, ::cpa, ::dec, ::dcp, ::iny, ::cpa, ::dex, ::axs, ::cpy, ::cpa, ::dec, ::dcp,
            ::bne, ::cpa, ::hlt, ::dcp, ::nop, ::cpa, ::dec, ::dcp, ::cld, ::cpa, ::nop, ::dcp, ::nop, ::cpa, ::dec, ::dcp,
            ::cpx, ::sbc, ::nop, ::isb, ::cpx, ::sbc, ::inc, ::isb, ::inx, ::sbc, ::nop, ::sbc, ::cpx, ::sbc, ::inc, ::isb,
            ::beq, ::sbc, ::hlt, ::isb, ::nop, ::sbc, ::inc, ::isb, ::sed, ::sbc, ::nop, ::isb, ::nop, ::sbc, ::inc, ::isb
        )
        // @formatter:on
    }

    inline var a: UByte
        get() = state.a
        private set(value) {
            state.a = setRegister(value)
        }

    inline var x: UByte
        get() = state.x
        private set(value) {
            state.x = setRegister(value)
        }

    inline var y: UByte
        get() = state.y
        private set(value) {
            state.y = setRegister(value)
        }

    inline var ps: UByte
        get() = state.ps
        private set(value) {
            state.ps = value and 0xCFU
        }

    inline var pc: UShort
        get() = state.pc
        private set(value) {
            state.pc = value
        }

    inline var sp: UByte
        get() = state.sp
        private set(value) {
            state.sp = value
        }

    private inline fun fetchOperandValue(): UByte {
        return if (addressMode.ordinal >= AddressMode.ZERO.ordinal) read(operand)
        else operand.toUByte()
    }

    inline fun clearIRQSource(source: IRQSource) {
        state.irq = state.irq and source.code.toUInt().inv()
    }

    inline fun setIRQSource(source: IRQSource) {
        state.irq = state.irq or source.code.toUInt()
    }

    inline fun hasIRQSource(source: IRQSource): Boolean {
        return (state.irq and source.code.toUInt()) != 0U
    }

    private inline fun setRegister(value: UByte): UByte {
        clearFlag(PSFlag.ZERO)
        clearFlag(PSFlag.NEGATIVE)
        setZeroNegativeFlags(value)
        return value
    }

    private inline fun setZeroNegativeFlags(value: UByte) {
        if (value.isZero) {
            setFlag(PSFlag.ZERO)
        } else if (value.bit7) {
            setFlag(PSFlag.NEGATIVE)
        }
    }

    private inline fun aac() {
        a = a and fetchOperandValue()

        clearFlag(PSFlag.CARRY)

        if (checkFlag(PSFlag.NEGATIVE)) {
            setFlag(PSFlag.CARRY)
        }
    }

    private inline fun adc() {
        add(fetchOperandValue())
    }

    private inline fun add(value: UByte) {
        val result = (a + value + if (checkFlag(PSFlag.CARRY)) 0x01U else 0x00U).toUShort()

        clearFlag(PSFlag.CARRY)
        clearFlag(PSFlag.NEGATIVE)
        clearFlag(PSFlag.OVERFLOW)
        clearFlag(PSFlag.ZERO)

        setZeroNegativeFlags(result.toUByte())

        // if(~(A() ^ value) & (A() ^ result) & 0x80)
        if (((a xor value).inv() and (a xor result.toUByte())).bit7) {
            setFlag(PSFlag.OVERFLOW)
        }
        if (result > 0xFFU) {
            setFlag(PSFlag.CARRY)
        }

        a = result.toUByte()
    }

    private inline fun and() {
        a = a and fetchOperandValue()
    }

    private inline fun arr() {
        a = ((a and fetchOperandValue()) shr 1) or if (checkFlag(PSFlag.CARRY)) 0x80U else 0x00U

        clearFlag(PSFlag.CARRY)
        clearFlag(PSFlag.OVERFLOW)

        if (a.bit6) {
            setFlag(PSFlag.CARRY)
        }

        if (checkFlag(PSFlag.CARRY) xor a.bit5) {
            setFlag(PSFlag.OVERFLOW)
        }
    }

    private inline fun asl(value: UByte): UByte {
        clearFlag(PSFlag.CARRY)
        clearFlag(PSFlag.NEGATIVE)
        clearFlag(PSFlag.ZERO)

        if (value.bit7) {
            setFlag(PSFlag.CARRY)
        }

        val result = (value.toUInt() shl 1).toUByte()
        setZeroNegativeFlags(result)
        return result
    }

    private inline fun aslAcc() {
        a = asl(a)
    }

    private inline fun aslMem() {
        val addr = operand
        val value = read(addr)
        write(addr, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        write(addr, asl(value))
    }

    private inline fun asr() {
        clearFlag(PSFlag.CARRY)

        a = a and fetchOperandValue()

        if (a.bit0) {
            setFlag(PSFlag.CARRY)
        }

        a = a shr 1
    }

    private inline fun atx() {
        // LDA & TAX
        a = fetchOperandValue() // LDA
        x = a // TAX
        a = a // Update flags based on A
    }

    private inline fun axa() {
        val addr = operand
        // This opcode stores the result of A AND X AND the high byte of the target address of the operand +1 in memory.
        // This may not be the actual behavior, but the read/write operations are needed for proper cycle counting
        write(operand, ((addr.hiByte + 1U) and a.toUInt() and x.toUInt()).toUByte())
    }

    private inline fun axs() {
        // CMP & DEX
        val value = fetchOperandValue()
        val ax = a and x
        val result = (ax - value).toUByte()

        clearFlag(PSFlag.CARRY)

        if (ax >= value) {
            setFlag(PSFlag.CARRY)
        }

        x = result
    }

    private inline fun branchRelative(branch: Boolean) {
        if (branch) {
            val offset = operand.toByte()

            // A taken non-page-crossing branch ignores IRQ/NMI during its last clock, so that next instruction executes before the IRQ
            // Fixes "branch_delays_irq" test
            if (runIrq && !prevRunIrq) {
                runIrq = false
            }

            dummyRead()

            if (isPageCrossed(pc, offset)) {
                dummyRead()
            }

            pc = (pc.toInt() + offset).toUShort()
        }
    }

    private inline fun bcc() {
        branchRelative(!checkFlag(PSFlag.CARRY))
    }

    private inline fun bcs() {
        branchRelative(checkFlag(PSFlag.CARRY))
    }

    private inline fun beq() {
        branchRelative(checkFlag(PSFlag.ZERO))
    }

    private inline fun bit() {
        val value = fetchOperandValue()

        clearFlag(PSFlag.ZERO)
        clearFlag(PSFlag.OVERFLOW)
        clearFlag(PSFlag.NEGATIVE)

        if ((a and value).isZero) {
            setFlag(PSFlag.ZERO)
        }
        if (value.bit6) {
            setFlag(PSFlag.OVERFLOW)
        }
        if (value.bit7) {
            setFlag(PSFlag.NEGATIVE)
        }
    }

    private inline fun bmi() {
        branchRelative(checkFlag(PSFlag.NEGATIVE))
    }

    private inline fun bne() {
        branchRelative(!checkFlag(PSFlag.ZERO))
    }

    private inline fun bpl() {
        branchRelative(!checkFlag(PSFlag.NEGATIVE))
    }

    private inline fun brk() {
        push(pc.plusOne())

        val flags = ps or PSFlag.BREAK.code or PSFlag.RESERVED.code

        if (isNeedNmi) {
            isNeedNmi = false

            push(flags)
            setFlag(PSFlag.INTERRUPT)

            pc = readWord(NMI_VECTOR)
        } else {
            push(flags)
            setFlag(PSFlag.INTERRUPT)

            pc = readWord(IRQ_VECTOR)
        }

        // Ensure we don't start an NMI right after running a BRK instruction (first instruction in IRQ handler must run first - needed for nmi_and_brk test)
        prevIsNeedNmi = false
    }

    private inline fun bvc() {
        branchRelative(!checkFlag(PSFlag.OVERFLOW))
    }

    private inline fun bvs() {
        branchRelative(checkFlag(PSFlag.OVERFLOW))
    }

    private inline fun clc() {
        clearFlag(PSFlag.CARRY)
    }

    private inline fun cld() {
        clearFlag(PSFlag.DECIMAL)
    }

    private inline fun cli() {
        clearFlag(PSFlag.INTERRUPT)
    }

    private inline fun clv() {
        clearFlag(PSFlag.OVERFLOW)
    }

    private inline fun cpa() {
        cmp(a, fetchOperandValue())
    }

    private inline fun cpx() {
        cmp(x, fetchOperandValue())
    }

    private inline fun cpy() {
        cmp(y, fetchOperandValue())
    }

    private inline fun dcp() {
        //DEC & CMP
        var value = fetchOperandValue()
        write(operand, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        value--
        cmp(a, value)
        write(operand, value)
    }

    private inline fun dec() {
        val addr = operand

        clearFlag(PSFlag.NEGATIVE)
        clearFlag(PSFlag.ZERO)

        var value = read(addr)

        write(addr, value, MemoryOperationType.DUMMY_WRITE) // Dummy write

        value--
        setZeroNegativeFlags(value)
        write(addr, value)
    }

    private inline fun dex() {
        x--
    }

    private inline fun dey() {
        y--
    }

    private inline fun eor() {
        a = a xor fetchOperandValue()
    }

    @Suppress("UNUSED_VARIABLE")
    private inline fun hlt() {
        // Normally freezes the cpu, we can probably assume nothing will ever call this
        val value = fetchOperandValue()
    }

    private inline fun inc() {
        val addr = operand

        clearFlag(PSFlag.NEGATIVE)
        clearFlag(PSFlag.ZERO)

        var value = read(addr)

        write(addr, value, MemoryOperationType.DUMMY_WRITE) // Dummy write

        value++
        setZeroNegativeFlags(value)
        write(addr, value)
    }

    private inline fun inx() {
        x++
    }

    private inline fun iny() {
        y++
    }

    private inline fun isb() {
        // INC & SBC
        var value = fetchOperandValue()
        write(operand, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        value++
        add(value xor 0xFFU)
        write(operand, value)
    }

    private inline fun jmp(addr: UShort) {
        pc = addr
    }

    private inline fun jmpAbs() {
        jmp(operand)
    }

    private inline fun jmpInd() {
        jmp(getInd())
    }

    private inline fun jsr() {
        val addr = operand
        dummyRead()
        push(pc.minusOne())
        jmp(addr)
    }

    private inline fun las() {
        // AND memory with stack pointer, transfer result to accumulator,
        // X register and stack pointer.
        val value = fetchOperandValue()
        a = value and sp
        x = a
        sp = a
    }

    private inline fun lax() {
        // LDA & LDX
        val value = fetchOperandValue()
        x = value
        a = value
    }

    private inline fun lda() {
        a = fetchOperandValue()
    }

    private inline fun ldx() {
        x = fetchOperandValue()
    }

    private inline fun ldy() {
        y = fetchOperandValue()
    }

    private inline fun lsr(value: UByte): UByte {
        clearFlag(PSFlag.CARRY)
        clearFlag(PSFlag.NEGATIVE)
        clearFlag(PSFlag.ZERO)

        if (value.bit0) {
            setFlag(PSFlag.CARRY)
        }

        val result = value shr 1
        setZeroNegativeFlags(result)
        return result
    }

    private inline fun lsrAcc() {
        a = lsr(a)
    }

    private inline fun lsrMem() {
        val addr = operand
        val value = read(addr)
        write(addr, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        write(addr, lsr(value))
    }

    @Suppress("UNUSED_VARIABLE")
    private inline fun nop() {
        // Make sure the nop operation takes as many cycles as meant to
        val value = fetchOperandValue()
    }

    private inline fun ora() {
        a = a or fetchOperandValue()
    }

    private inline fun pha() {
        push(a)
    }

    private inline fun php() {
        push(ps or PSFlag.BREAK.code or PSFlag.RESERVED.code)
    }

    private inline fun pla() {
        dummyRead()
        a = pop()
    }

    private inline fun plp() {
        dummyRead()
        ps = pop()
    }

    private inline fun rla() {
        // LSR & EOR
        val value = fetchOperandValue()
        write(operand, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        val shiftedValue = rol(value)
        a = a and shiftedValue
        write(operand, shiftedValue)
    }

    private inline fun rol(value: UByte): UByte {
        val isCarry = checkFlag(PSFlag.CARRY)

        clearFlag(PSFlag.CARRY)
        clearFlag(PSFlag.NEGATIVE)
        clearFlag(PSFlag.ZERO)

        if (value.bit7) {
            setFlag(PSFlag.CARRY)
        }

        val result = (value.toUInt() shl 1).toUByte() or if (isCarry) 0x01U else 0x00U
        setZeroNegativeFlags(result)
        return result
    }

    private inline fun rolAcc() {
        a = rol(a)
    }

    private inline fun rolMem() {
        val addr = operand
        val value = read(addr)
        write(addr, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        write(addr, rol(value))
    }

    private inline fun ror(value: UByte): UByte {
        val isCarry = checkFlag(PSFlag.CARRY)

        clearFlag(PSFlag.CARRY)
        clearFlag(PSFlag.NEGATIVE)
        clearFlag(PSFlag.ZERO)

        if (value.bit0) {
            setFlag(PSFlag.CARRY)
        }

        val result = (value shr 1) or if (isCarry) 0x80U else 0x00U
        setZeroNegativeFlags(result)
        return result
    }

    private inline fun rorAcc() {
        a = ror(a)
    }

    private inline fun rorMem() {
        val addr = operand
        val value = read(addr)
        write(addr, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        write(addr, ror(value))
    }

    private inline fun rra() {
        // ROR & ADC
        val value = fetchOperandValue()
        write(operand, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        val shiftedValue = ror(value)
        add(shiftedValue)
        write(operand, shiftedValue)
    }

    private inline fun rti() {
        dummyRead()
        ps = pop()
        pc = popWord()
    }

    private inline fun rts() {
        val addr = popWord()
        dummyRead()
        dummyRead()
        pc = (addr + 1U).toUShort()
    }

    private inline fun sax() {
        // STA & STX
        write(operand, a and x)
    }

    private inline fun sbc() {
        add(fetchOperandValue() xor 0xFFU)
    }

    private inline fun sec() {
        setFlag(PSFlag.CARRY)
    }

    private inline fun sed() {
        setFlag(PSFlag.DECIMAL)
    }

    private inline fun sei() {
        setFlag(PSFlag.INTERRUPT)
    }

    private inline fun slo() {
        // ASL & ORA
        val value = fetchOperandValue()
        write(operand, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        val shiftedValue = asl(value)
        a = a or shiftedValue
        write(operand, shiftedValue)
    }

    private inline fun sre() {
        // ROL & AND
        val value = fetchOperandValue()
        write(operand, value, MemoryOperationType.DUMMY_WRITE) // Dummy write
        val shiftedValue = lsr(value)
        a = a xor shiftedValue
        write(operand, shiftedValue)
    }

    private inline fun sta() {
        write(operand, a)
    }

    private inline fun stx() {
        write(operand, x)
    }

    private inline fun sty() {
        write(operand, y)
    }

    private inline fun sxa() {
        val hi = operand.hiByte
        val lo = operand.loByte
        val value = x and hi.plusOne()

        write((((x.toUInt() and (hi + 1U)) shl 8) or lo.toUInt()).toUShort(), value)
    }

    private inline fun sya() {
        val hi = operand.hiByte
        val lo = operand.loByte
        val value = y and hi.plusOne()

        // From here: http://forums.nesdev.com/viewtopic.php?f=3&t=3831&start=30
        // Unsure if this is accurate or not
        // the target address for e.g. SYA becomes ((y & (addr_high + 1)) << 8) | addr_low instead of the normal ((addr_high + 1) << 8) | addr_low
        write((((y.toUInt() and (hi + 1U)) shl 8) or lo.toUInt()).toUShort(), value)
    }

    private inline fun tas() {
        // AND X register with accumulator and store result in stack
        // pointer, then AND stack pointer with the high byte of the
        // target address of the argument + 1. Store result in memory.
        val addr = operand
        sp = x and a
        write(addr, sp and addr.hiByte.plusOne())
    }

    private inline fun tax() {
        x = a
    }

    private inline fun tay() {
        y = a
    }

    private inline fun tsx() {
        x = sp
    }

    private inline fun txa() {
        a = x
    }

    private inline fun txs() {
        sp = x
    }

    private inline fun tya() {
        a = y
    }

    @Suppress("UNUSED_VARIABLE")
    private inline fun unk() {
        // Make sure we take the right amount of cycles (not reliable for operations that write to memory, etc.)
        val value = fetchOperandValue()
    }

    private inline fun cmp(reg: UByte, value: UByte) {
        clearFlag(PSFlag.CARRY)
        clearFlag(PSFlag.NEGATIVE)
        clearFlag(PSFlag.ZERO)

        val result = reg - value

        if (reg >= value) {
            setFlag(PSFlag.CARRY)
        }
        if (reg == value) {
            setFlag(PSFlag.ZERO)
        }
        if (result and 0x80U == 0x80U) {
            setFlag(PSFlag.NEGATIVE)
        }
    }

    fun reset(softReset: Boolean, region: Region) {
        state.nmi = false
        state.irq = 0U

        spriteDmaTransfer = false
        spriteDmaOffset = 0U
        isNeedHalt = false
        dmcDmaRunning = false

        // Used by NSF code to disable Frame Counter & DMC interrupts
        irqMask = 0xFFU

        // Use _memoryManager->Read() directly to prevent clocking the PPU/APU when setting PC at reset
        state.pc = makeUShort(
            memoryManager.read(RESET_VECTOR),
            memoryManager.read(RESET_VECTOR.plusOne())
        )

        if (softReset) {
            setFlag(PSFlag.INTERRUPT)
            state.sp = (state.sp - 0x03U).toUByte()
        } else {
            state.a = 0U
            state.sp = 0xFDU
            state.x = 0U
            state.y = 0U
            state.ps = PSFlag.INTERRUPT.code

            runIrq = false
        }

        val ppuDivider: Int
        val cpuDivider: Int

        when (region) {
            Region.PAL -> {
                ppuDivider = 5
                cpuDivider = 16
            }
            Region.DENDY -> {
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

        if (console.settings.checkFlag(EmulationFlag.RANDOMIZE_CPU_PPU_ALIGNMENT)) {
            ppuOffset = Random.nextInt(ppuDivider).toUByte()
            cpuOffset = Random.nextInt(cpuDivider)
        } else {
            ppuOffset = 1U
            cpuOffset = 0
        }

        masterClock += cpuDivider + cpuOffset

        // The CPU takes 8 cycles before it starts executing the ROM's code after a reset/power up
        for (i in 0..7) {
            startCpuCycle(true)
            endCpuCycle(true)
        }
    }

    private inline fun readOpcode(): UByte {
        return read(state.pc++, MemoryOperationType.OPCODE)
    }

    private inline fun dummyRead() {
        read(state.pc, MemoryOperationType.DUMMY_READ)
    }

    private inline fun readByte(): UByte {
        return read(state.pc++, MemoryOperationType.OPERAND)
    }

    private inline fun readWord(): UShort {
        val value = readWord(state.pc, MemoryOperationType.OPERAND)
        state.pc++
        state.pc++
        return value
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        processPendingDma(addr)
        startCpuCycle(true)
        val value = memoryManager.read(addr, type)
        endCpuCycle(true)
        return value
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        isCpuWrite = true
        startCpuCycle(false)
        memoryManager.write(addr, value, type)
        endCpuCycle(false)
        isCpuWrite = false
    }

    private inline fun push(value: UByte) {
        write((sp + 0x100U).toUShort(), value)
        sp--
    }

    private inline fun push(value: UShort) {
        push(value.hiByte)
        push(value.loByte)
    }

    private inline fun pop(): UByte {
        sp++
        return read((0x100U + sp).toUShort())
    }

    private inline fun popWord(): UShort {
        val lo = pop()
        val hi = pop()
        return makeUShort(lo, hi)
    }

    private inline fun processCycle() {
        // Sprite DMA cycles count as halt/dummy cycles for the DMC DMA when both run at the same time
        if (isNeedHalt) {
            isNeedHalt = false
        } else if (isNeedDummyRead) {
            isNeedDummyRead = false
        }

        startCpuCycle(true)
    }

    private inline fun processPendingDma(addr: UShort) {
        if (!isNeedHalt) return

        startCpuCycle(true)
        memoryManager.read(addr, MemoryOperationType.DUMMY_READ)
        endCpuCycle(true)

        isNeedHalt = false

        var spriteDmaCounter: UShort = 0U
        var spriteReadAddr: UByte = 0U
        var readValue: UByte = 0U
        val skipDummyReads = addr.toUInt() == 0x4016U || addr.toUInt() == 0x4017U

        while (dmcDmaRunning || spriteDmaTransfer) {
            val isCycle = (cycleCount and 0x01) == 0L

            if (isCycle) {
                if (dmcDmaRunning && !isNeedHalt && !isNeedDummyRead) {
                    // DMC DMA is ready to read a byte (both halt and dummy read cycles were performed before this)
                    processCycle()
                    readValue = memoryManager.read(console.apu.dmcReadAddress, MemoryOperationType.DMC_READ)
                    endCpuCycle(true)
                    console.apu.setDmcReadBuffer(readValue)
                    dmcDmaRunning = false
                } else if (spriteDmaTransfer) {
                    // DMC DMA is not running, or not ready, run sprite DMA
                    processCycle()
                    readValue = memoryManager.read((spriteDmaOffset * 0x100U + spriteReadAddr).toUShort())
                    endCpuCycle(true)
                    spriteReadAddr++
                    spriteDmaCounter++
                } else {
                    // DMC DMA is running, but not ready (need halt/dummy read) and sprite DMA isn't runnnig, perform a dummy read
                    // assert(isNeedHalt || isNeedDummyRead)
                    processCycle()

                    if (!skipDummyReads) {
                        memoryManager.read(addr, MemoryOperationType.DUMMY_READ)
                    }

                    endCpuCycle(true)
                }
            } else {
                if (spriteDmaTransfer && spriteDmaCounter.bit0) {
                    //Sprite DMA write cycle (only do this if a sprite dma read was performed last cycle)
                    processCycle()
                    memoryManager.write(0x2004U, readValue)
                    endCpuCycle(true)
                    spriteDmaCounter++

                    if (spriteDmaCounter.toUInt() == 0x200U) {
                        spriteDmaTransfer = false
                    }
                } else {
                    // Align to read cycle before starting sprite DMA (or align to perform DMC read)
                    processCycle()

                    if (!skipDummyReads) {
                        memoryManager.read(addr, MemoryOperationType.DUMMY_READ)
                    }

                    endCpuCycle(true)
                }
            }
        }
    }

    private inline fun startCpuCycle(forRead: Boolean) {
        masterClock += if (forRead) startClockCount.toInt() - 1 else startClockCount.toInt() + 1
        cycleCount++
        console.ppu.run(masterClock - ppuOffset.toInt())
        console.processCpuClock()
    }

    private inline fun endCpuCycle(forRead: Boolean) {
        masterClock += if (forRead) endClockCount.toInt() + 1 else endClockCount.toInt() - 1
        console.ppu.run(masterClock - ppuOffset.toInt())

        // The internal signal goes high during φ1 of the cycle that follows the one where the edge is detected,
        // and stays high until the NMI has been handled.
        prevIsNeedNmi = isNeedNmi

        // This edge detector polls the status of the NMI line during φ2 of each CPU cycle (i.e., during the
        // second half of each cycle) and raises an internal signal if the input goes from being high during
        // one cycle to being low during the next
        if (!prevNmiFlag && state.nmi) {
            isNeedNmi = true
        }

        prevNmiFlag = state.nmi

        // it's really the status of the interrupt lines at the end of the second-to-last cycle that matters.
        // Keep the irq lines values from the previous cycle.  The before-to-last cycle's values will be used
        prevRunIrq = runIrq
        runIrq = (state.irq and irqMask.toUInt()) > 0U && !checkFlag(PSFlag.INTERRUPT)
    }

    private inline fun checkFlag(flag: PSFlag): Boolean {
        return state.ps and flag.code == flag.code
    }

    private inline fun clearFlag(flag: PSFlag) {
        state.ps = state.ps and flag.code.inv()
    }

    private inline fun setFlag(flag: PSFlag) {
        state.ps = state.ps or flag.code
    }

    fun runDmaTransfer(offset: UByte) {
        spriteDmaTransfer = true
        spriteDmaOffset = offset
        isNeedHalt = true
    }

    fun startDmcTransfer() {
        dmcDmaRunning = true
        isNeedDummyRead = true
        isNeedHalt = true
    }

    fun exec() {
        val opcode = readOpcode().toInt()
        addressMode = ADDRESS_MODES[opcode]
        operand = fetchOperand()

        opTable[opcode]()

        if (prevRunIrq || prevIsNeedNmi) {
            irq()
        }
    }

    private inline fun fetchOperand(): UShort {
        when (addressMode) {
            AddressMode.ACC,
            AddressMode.IMP -> {
                dummyRead()
                return 0U
            }
            AddressMode.IMM,
            AddressMode.REL -> return getImmediate().toUShort()
            AddressMode.ZERO -> return getZeroAddr().toUShort()
            AddressMode.ZERO_X -> return getZeroXAddr().toUShort()
            AddressMode.ZERO_Y -> return getZeroYAddr().toUShort()
            AddressMode.IND -> return getIndAddr()
            AddressMode.IND_X -> return getIndXAddr()
            AddressMode.IND_Y -> return getIndYAddr(false)
            AddressMode.IND_YW -> return getIndYAddr(true)
            AddressMode.ABS -> return getAbsAddr()
            AddressMode.ABS_X -> return getAbsXAddr(false)
            AddressMode.ABS_XW -> return getAbsXAddr(true)
            AddressMode.ABS_Y -> return getAbsYAddr(false)
            AddressMode.ABS_YW -> return getAbsYAddr(true)
            AddressMode.NONE -> {
            }
        }

        // if (console.settings.checkFlag(EmulationFlag.BreakOnCrash)) {
        // When "Break on Crash" is enabled, open the debugger and break immediately if a crash occurs
        // }

        if (console.isNsf) {
            // Don't stop emulation on CPU crash when playing NSFs, reset cpu instead
            console.reset(true)
        }

        return 0U
    }

    private inline fun irq() {
        // Fetch opcode (and discard it - $00 (BRK) is forced into the opcode register instead)
        dummyRead()
        // Read next instruction byte (actually the same as above, since PC increment is suppressed. Also discarded.)
        dummyRead()

        push(pc)

        if (isNeedNmi) {
            isNeedNmi = false

            push((this.ps or PSFlag.RESERVED.code))
            setFlag(PSFlag.INTERRUPT)

            pc = readWord(NMI_VECTOR)
        } else {
            push((this.ps or PSFlag.RESERVED.code))
            setFlag(PSFlag.INTERRUPT)

            pc = readWord(IRQ_VECTOR)
        }
    }

    private inline fun getImmediate() = readByte()

    private inline fun getIndAddr() = readWord()

    private inline fun getZeroAddr() = readByte()

    private inline fun getZeroXAddr(): UByte {
        val value = readByte()
        read(value.toUShort(), MemoryOperationType.DUMMY_READ)
        return (value + x).toUByte()
    }

    private inline fun getZeroYAddr(): UByte {
        val value = readByte()
        read(value.toUShort(), MemoryOperationType.DUMMY_READ)
        return (value + y).toUByte()
    }

    private inline fun getAbsAddr() = readWord()

    private inline fun getAbsXAddr(dummyRead: Boolean): UShort {
        val base = readWord()
        val pageCrossed = isPageCrossed(base, x)

        if (pageCrossed || dummyRead) {
            // Dummy read done by the processor (only when page is crossed for READ instructions)
            read((base + x - if (pageCrossed) 0x100U else 0U).toUShort(), MemoryOperationType.DUMMY_READ)
        }

        return (base + x).toUShort()
    }

    private inline fun getAbsYAddr(dummyRead: Boolean): UShort {
        val base = readWord()
        val pageCrossed = isPageCrossed(base, y)

        if (pageCrossed || dummyRead) {
            // Dummy read done by the processor (only when page is crossed for READ instructions)
            read((base + y - if (pageCrossed) 0x100U else 0U).toUShort(), MemoryOperationType.DUMMY_READ)
        }

        return (base + y).toUShort()
    }

    private inline fun getInd(): UShort {
        val addr = operand

        return if (addr.toUInt() and 0xFFU == 0xFFU) {
            val lo = read(addr)
            val hi = read((addr - 0xFFU).toUShort())
            makeUShort(lo, hi)
        } else {
            readWord(addr)
        }
    }

    private inline fun getIndXAddr(): UShort {
        var zero = readByte()

        // Dummy read
        read(zero.toUShort(), MemoryOperationType.DUMMY_READ)

        zero = (zero + x).toUByte()

        return if (zero.toUInt() == 0xFFU) {
            val lo = read(0xFFU)
            val hi = read(0x00U)
            makeUShort(lo, hi)
        } else {
            readWord(zero.toUShort())
        }
    }

    private inline fun getIndYAddr(dummyRead: Boolean): UShort {
        val zero = readByte()

        val addr = if (zero.toUInt() == 0xFFU) {
            val lo = read(0xFFU)
            val hi = read(0x00U)
            makeUShort(lo, hi)
        } else {
            readWord(zero.toUShort())
        }

        val pageCrossed = isPageCrossed(addr, y)

        if (pageCrossed || dummyRead) {
            // Dummy read done by the processor (only when page is crossed for READ instructions)
            read((addr + y - if (pageCrossed) 0x100U else 0U).toUShort(), MemoryOperationType.DUMMY_READ)
        }

        return (addr + y).toUShort()
    }

    private inline fun isPageCrossed(a: UShort, b: Byte): Boolean {
        return ((a.toInt() + b) and 0xFF00) != (a.toInt() and 0xFF00)
    }

    private inline fun isPageCrossed(a: UShort, b: UByte): Boolean {
        return ((a + b) and 0xFF00U) != (a.toUInt() and 0xFF00U)
    }

    fun setMasterClockDivider(region: Region) {
        when (region) {
            Region.PAL -> {
                startClockCount = 8U
                endClockCount = 8U
            }
            Region.DENDY -> {
                startClockCount = 7U
                endClockCount = 8U
            }
            else -> {
                startClockCount = 6U
                endClockCount = 6U
            }
        }
    }

    inline var nmi: Boolean
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
        s.write("isNeedDummyRead", isNeedDummyRead)
        s.write("isNeedHalt", isNeedHalt)
        s.write("startClockCount", startClockCount)
        s.write("endClockCount", endClockCount)
        s.write("ppuOffset", ppuOffset)
        s.write("masterClock", masterClock)
        s.write("prevIsNeedNmi", prevIsNeedNmi)
        s.write("prevNmiFlag", prevNmiFlag)
        s.write("isNeedNmi", isNeedNmi)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        s.readSnapshot("state")?.let { state.restoreState(it) }
        cycleCount = s.readLong("cycleCount") ?: -1L
        dmcDmaRunning = s.readBoolean("dmcDmaRunning") ?: false
        spriteDmaTransfer = s.readBoolean("spriteDmaTransfer") ?: false
        val extraScanlinesBeforeNmi = s.readInt("extraScanlinesBeforeNmi") ?: 0
        val extraScanlinesAfterNmi = s.readInt("extraScanlinesAfterNmi") ?: 0
        val dipSwitches = s.readInt("dipSwitches") ?: 0
        isNeedDummyRead = s.readBoolean("isNeedDummyRead") ?: false
        isNeedHalt = s.readBoolean("isNeedHalt") ?: false
        startClockCount = s.readUByte("startClockCount") ?: 0U
        endClockCount = s.readUByte("endClockCount") ?: 0U
        ppuOffset = s.readUByte("ppuOffset") ?: 0U
        masterClock = s.readLong("masterClock") ?: 0L
        prevIsNeedNmi = s.readBoolean("prevIsNeedNmi") ?: false
        prevNmiFlag = s.readBoolean("prevNmiFlag") ?: false
        isNeedNmi = s.readBoolean("isNeedNmi") ?: false

        console.settings.extraScanlinesAfterNmi = extraScanlinesAfterNmi
        console.settings.extraScanlinesBeforeNmi = extraScanlinesBeforeNmi
        console.settings.dipSwitches = dipSwitches
    }

    companion object {
        // @formatter:off
        private val ADDRESS_MODES = arrayOf(
            AddressMode.IMP, AddressMode.IND_X, AddressMode.NONE, AddressMode.IND_X, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.IMP, AddressMode.IMM, AddressMode.ACC, AddressMode.IMM, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.ABS, AddressMode.IND_X, AddressMode.NONE, AddressMode.IND_X, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.IMP, AddressMode.IMM, AddressMode.ACC, AddressMode.IMM, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.IMP, AddressMode.IND_X, AddressMode.NONE, AddressMode.IND_X, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.IMP, AddressMode.IMM, AddressMode.ACC, AddressMode.IMM, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.IMP, AddressMode.IND_X, AddressMode.NONE, AddressMode.IND_X, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.IMP, AddressMode.IMM, AddressMode.ACC, AddressMode.IMM, AddressMode.IND, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.IMM, AddressMode.IND_X, AddressMode.IMM, AddressMode.IND_X, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.IMP, AddressMode.IMM, AddressMode.IMP, AddressMode.IMM, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_YW, AddressMode.NONE, AddressMode.IND_YW, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_Y, AddressMode.ZERO_Y, AddressMode.IMP, AddressMode.ABS_YW,AddressMode.IMP, AddressMode.ABS_YW,AddressMode.ABS_XW,AddressMode.ABS_XW,AddressMode.ABS_YW, AddressMode.ABS_YW,
            AddressMode.IMM, AddressMode.IND_X, AddressMode.IMM, AddressMode.IND_X, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.IMP, AddressMode.IMM, AddressMode.IMP, AddressMode.IMM, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_Y, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_Y, AddressMode.ZERO_Y, AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_Y, AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_Y, AddressMode.ABS_Y,
            AddressMode.IMM, AddressMode.IND_X, AddressMode.IMM, AddressMode.IND_X, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.IMP, AddressMode.IMM, AddressMode.IMP, AddressMode.IMM, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
            AddressMode.IMM, AddressMode.IND_X, AddressMode.IMM, AddressMode.IND_X, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.ZERO, AddressMode.IMP, AddressMode.IMM, AddressMode.IMP, AddressMode.IMM, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS, AddressMode.ABS,
            AddressMode.REL, AddressMode.IND_Y, AddressMode.NONE, AddressMode.IND_YW, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.ZERO_X, AddressMode.IMP, AddressMode.ABS_Y, AddressMode.IMP, AddressMode.ABS_YW,AddressMode.ABS_X, AddressMode.ABS_X, AddressMode.ABS_XW, AddressMode.ABS_XW,
        )

        val OPCODE_NAMES = listOf(
            "BRK", "ORA", "STP", "SLO", "NOP", "ORA", "ASL", "SLO", "PHP", "ORA", "ASL", "ANC", "NOP", "ORA", "ASL", "SLO", 
            "BPL", "ORA", "STP", "SLO", "NOP", "ORA", "ASL", "SLO", "CLC", "ORA", "NOP", "SLO", "NOP", "ORA", "ASL", "SLO", 
            "JSR", "AND", "STP", "RLA", "BIT", "AND", "ROL", "RLA", "PLP", "AND", "ROL", "ANC", "BIT", "AND", "ROL", "RLA", 
            "BMI", "AND", "STP", "RLA", "NOP", "AND", "ROL", "RLA", "SEC", "AND", "NOP", "RLA", "NOP", "AND", "ROL", "RLA", 
            "RTI", "EOR", "STP", "SRE", "NOP", "EOR", "LSR", "SRE", "PHA", "EOR", "LSR", "ALR", "JMP", "EOR", "LSR", "SRE", 
            "BVC", "EOR", "STP", "SRE", "NOP", "EOR", "LSR", "SRE", "CLI", "EOR", "NOP", "SRE", "NOP", "EOR", "LSR", "SRE", 
            "RTS", "ADC", "STP", "RRA", "NOP", "ADC", "ROR", "RRA", "PLA", "ADC", "ROR", "ARR", "JMP", "ADC", "ROR", "RRA", 
            "BVS", "ADC", "STP", "RRA", "NOP", "ADC", "ROR", "RRA", "SEI", "ADC", "NOP", "RRA", "NOP", "ADC", "ROR", "RRA", 
            "NOP", "STA", "NOP", "SAX", "STY", "STA", "STX", "SAX", "DEY", "NOP", "TXA", "XAA", "STY", "STA", "STX", "SAX", 
            "BCC", "STA", "STP", "AHX", "STY", "STA", "STX", "SAX", "TYA", "STA", "TXS", "TAS", "SHY", "STA", "SHX", "AXA", 
            "LDY", "LDA", "LDX", "LAX", "LDY", "LDA", "LDX", "LAX", "TAY", "LDA", "TAX", "LAX", "LDY", "LDA", "LDX", "LAX", 
            "BCS", "LDA", "STP", "LAX", "LDY", "LDA", "LDX", "LAX", "CLV", "LDA", "TSX", "LAS", "LDY", "LDA", "LDX", "LAX", 
            "CPY", "CMP", "NOP", "DCP", "CPY", "CMP", "DEC", "DCP", "INY", "CMP", "DEX", "AXS", "CPY", "CMP", "DEC", "DCP", 
            "BNE", "CMP", "STP", "DCP", "NOP", "CMP", "DEC", "DCP", "CLD", "CMP", "NOP", "DCP", "NOP", "CMP", "DEC", "DCP", 
            "CPX", "SBC", "NOP", "ISC", "CPX", "SBC", "INC", "ISC", "INX", "SBC", "NOP", "SBC", "CPX", "SBC", "INC", "ISC", 
            "BEQ", "SBC", "STP", "ISC", "NOP", "SBC", "INC", "ISC", "SED", "SBC", "NOP", "ISC", "NOP", "SBC", "INC", "ISC",
        )
        // @formatter:on

        const val NMI_VECTOR: UShort = 0xFFFAU
        const val RESET_VECTOR: UShort = 0xFFFCU
        const val IRQ_VECTOR: UShort = 0xFFFEU
        const val CLOCK_RATE_NTSC: Int = 1789773
        const val CLOCK_RATE_PAL: Int = 1662607
        const val CLOCK_RATE_DENDY: Int = 1773448
    }
}
