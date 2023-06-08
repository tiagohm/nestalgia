package br.tiagohm.nestalgia.core

data class CpuInstruction(private val cpu: Cpu) {

    fun interface Instruction : Runnable

    // @formatter:off
    private inner class Aac : Instruction { override fun run() = cpu.aac() }
    private inner class Adc : Instruction { override fun run() = cpu.adc() }
    private inner class And : Instruction { override fun run() = cpu.and() }
    private inner class Arr : Instruction { override fun run() = cpu.arr() }
    private inner class AslAcc : Instruction { override fun run() = cpu.aslAcc() }
    private inner class AslMem : Instruction { override fun run() = cpu.aslMem() }
    private inner class Asr : Instruction { override fun run() = cpu.asr() }
    private inner class Atx : Instruction { override fun run() = cpu.atx() }
    private inner class Axa : Instruction { override fun run() = cpu.axa() }
    private inner class Axs : Instruction { override fun run() = cpu.axs() }
    private inner class Bcc : Instruction { override fun run() = cpu.bcc() }
    private inner class Bcs : Instruction { override fun run() = cpu.bcs() }
    private inner class Beq : Instruction { override fun run() = cpu.beq() }
    private inner class Bit : Instruction { override fun run() = cpu.bit() }
    private inner class Bmi : Instruction { override fun run() = cpu.bmi() }
    private inner class Bne : Instruction { override fun run() = cpu.bne() }
    private inner class Bpl : Instruction { override fun run() = cpu.bpl() }
    private inner class Brk : Instruction { override fun run() = cpu.brk() }
    private inner class Bvc : Instruction { override fun run() = cpu.bvc() }
    private inner class Bvs : Instruction { override fun run() = cpu.bvs() }
    private inner class Clc : Instruction { override fun run() = cpu.clc() }
    private inner class Cld : Instruction { override fun run() = cpu.cld() }
    private inner class Cli : Instruction { override fun run() = cpu.cli() }
    private inner class Clv : Instruction { override fun run() = cpu.clv() }
    private inner class Cpa : Instruction { override fun run() = cpu.cpa() }
    private inner class Cpx : Instruction { override fun run() = cpu.cpx() }
    private inner class Cpy : Instruction { override fun run() = cpu.cpy() }
    private inner class Dcp : Instruction { override fun run() = cpu.dcp() }
    private inner class Dec : Instruction { override fun run() = cpu.dec() }
    private inner class Dex : Instruction { override fun run() = cpu.dex() }
    private inner class Dey : Instruction { override fun run() = cpu.dey() }
    private inner class Eor : Instruction { override fun run() = cpu.eor() }
    private inner class Hlt : Instruction { override fun run() = cpu.hlt() }
    private inner class Inc : Instruction { override fun run() = cpu.inc() }
    private inner class Inx : Instruction { override fun run() = cpu.inx() }
    private inner class Iny : Instruction { override fun run() = cpu.iny() }
    private inner class Isb : Instruction { override fun run() = cpu.isb() }
    private inner class JmpAbs : Instruction { override fun run() = cpu.jmpAbs() }
    private inner class JmpInd : Instruction { override fun run() = cpu.jmpInd() }
    private inner class Jsr : Instruction { override fun run() = cpu.jsr() }
    private inner class Las : Instruction { override fun run() = cpu.las() }
    private inner class Lax : Instruction { override fun run() = cpu.lax() }
    private inner class Lda : Instruction { override fun run() = cpu.lda() }
    private inner class Ldx : Instruction { override fun run() = cpu.ldx() }
    private inner class Ldy : Instruction { override fun run() = cpu.ldy() }
    private inner class LsrAcc : Instruction { override fun run() = cpu.lsrAcc() }
    private inner class LsrMem : Instruction { override fun run() = cpu.lsrMem() }
    private inner class Nop : Instruction { override fun run() = cpu.nop() }
    private inner class Ora : Instruction { override fun run() = cpu.ora() }
    private inner class Pha : Instruction { override fun run() = cpu.pha() }
    private inner class Php : Instruction { override fun run() = cpu.php() }
    private inner class Pla : Instruction { override fun run() = cpu.pla() }
    private inner class Plp : Instruction { override fun run() = cpu.plp() }
    private inner class Rla : Instruction { override fun run() = cpu.rla() }
    private inner class RolAcc : Instruction { override fun run() = cpu.rolAcc() }
    private inner class RolMem : Instruction { override fun run() = cpu.rolMem() }
    private inner class RorAcc : Instruction { override fun run() = cpu.rorAcc() }
    private inner class RorMem : Instruction { override fun run() = cpu.rorMem() }
    private inner class Rra : Instruction { override fun run() = cpu.rra() }
    private inner class Rti : Instruction { override fun run() = cpu.rti() }
    private inner class Rts : Instruction { override fun run() = cpu.rts() }
    private inner class Sax : Instruction { override fun run() = cpu.sax() }
    private inner class Sbc : Instruction { override fun run() = cpu.sbc() }
    private inner class Sec : Instruction { override fun run() = cpu.sec() }
    private inner class Sed : Instruction { override fun run() = cpu.sed() }
    private inner class Sei : Instruction { override fun run() = cpu.sei() }
    private inner class Slo : Instruction { override fun run() = cpu.slo() }
    private inner class Sre : Instruction { override fun run() = cpu.sre() }
    private inner class Sta : Instruction { override fun run() = cpu.sta() }
    private inner class Stx : Instruction { override fun run() = cpu.stx() }
    private inner class Sty : Instruction { override fun run() = cpu.sty() }
    private inner class Sxa : Instruction { override fun run() = cpu.sxa() }
    private inner class Sya : Instruction { override fun run() = cpu.sya() }
    private inner class Tas : Instruction { override fun run() = cpu.tas() }
    private inner class Tax : Instruction { override fun run() = cpu.tax() }
    private inner class Tay : Instruction { override fun run() = cpu.tay() }
    private inner class Tsx : Instruction { override fun run() = cpu.tsx() }
    private inner class Txa : Instruction { override fun run() = cpu.txa() }
    private inner class Txs : Instruction { override fun run() = cpu.txs() }
    private inner class Tya : Instruction { override fun run() = cpu.tya() }
    private inner class Unk : Instruction { override fun run() = cpu.unk() }
    // @formatter:on

    @PublishedApi @JvmField internal val opTable = arrayOf(
        Brk(), Ora(), Hlt(), Slo(), Nop(), Ora(), AslMem(), Slo(),
        Php(), Ora(), AslAcc(), Aac(), Nop(), Ora(), AslMem(), Slo(),
        Bpl(), Ora(), Hlt(), Slo(), Nop(), Ora(), AslMem(), Slo(),
        Clc(), Ora(), Nop(), Slo(), Nop(), Ora(), AslMem(), Slo(),
        Jsr(), And(), Hlt(), Rla(), Bit(), And(), RolMem(), Rla(),
        Plp(), And(), RolAcc(), Aac(), Bit(), And(), RolMem(), Rla(),
        Bmi(), And(), Hlt(), Rla(), Nop(), And(), RolMem(), Rla(),
        Sec(), And(), Nop(), Rla(), Nop(), And(), RolMem(), Rla(),
        Rti(), Eor(), Hlt(), Sre(), Nop(), Eor(), LsrMem(), Sre(),
        Pha(), Eor(), LsrAcc(), Asr(), JmpAbs(), Eor(), LsrMem(), Sre(),
        Bvc(), Eor(), Hlt(), Sre(), Nop(), Eor(), LsrMem(), Sre(),
        Cli(), Eor(), Nop(), Sre(), Nop(), Eor(), LsrMem(), Sre(),
        Rts(), Adc(), Hlt(), Rra(), Nop(), Adc(), RorMem(), Rra(),
        Pla(), Adc(), RorAcc(), Arr(), JmpInd(), Adc(), RorMem(), Rra(),
        Bvs(), Adc(), Hlt(), Rra(), Nop(), Adc(), RorMem(), Rra(),
        Sei(), Adc(), Nop(), Rra(), Nop(), Adc(), RorMem(), Rra(),
        Nop(), Sta(), Nop(), Sax(), Sty(), Sta(), Stx(), Sax(),
        Dey(), Nop(), Txa(), Unk(), Sty(), Sta(), Stx(), Sax(),
        Bcc(), Sta(), Hlt(), Axa(), Sty(), Sta(), Stx(), Sax(),
        Tya(), Sta(), Txs(), Tas(), Sya(), Sta(), Sxa(), Axa(),
        Ldy(), Lda(), Ldx(), Lax(), Ldy(), Lda(), Ldx(), Lax(),
        Tay(), Lda(), Tax(), Atx(), Ldy(), Lda(), Ldx(), Lax(),
        Bcs(), Lda(), Hlt(), Lax(), Ldy(), Lda(), Ldx(), Lax(),
        Clv(), Lda(), Tsx(), Las(), Ldy(), Lda(), Ldx(), Lax(),
        Cpy(), Cpa(), Nop(), Dcp(), Cpy(), Cpa(), Dec(), Dcp(),
        Iny(), Cpa(), Dex(), Axs(), Cpy(), Cpa(), Dec(), Dcp(),
        Bne(), Cpa(), Hlt(), Dcp(), Nop(), Cpa(), Dec(), Dcp(),
        Cld(), Cpa(), Nop(), Dcp(), Nop(), Cpa(), Dec(), Dcp(),
        Cpx(), Sbc(), Nop(), Isb(), Cpx(), Sbc(), Inc(), Isb(),
        Inx(), Sbc(), Nop(), Sbc(), Cpx(), Sbc(), Inc(), Isb(),
        Beq(), Sbc(), Hlt(), Isb(), Nop(), Sbc(), Inc(), Isb(),
        Sed(), Sbc(), Nop(), Isb(), Nop(), Sbc(), Inc(), Isb(),
    )

    @Suppress("NOTHING_TO_INLINE")
    inline fun execute(opcode: Int) {
        opTable[opcode].run()
    }
}
