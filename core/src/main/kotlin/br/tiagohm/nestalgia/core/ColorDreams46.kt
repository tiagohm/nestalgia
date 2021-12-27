package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_046

@Suppress("NOTHING_TO_INLINE")
class ColorDreams46 : Mapper() {

    private val regs = UByteArray(2)

    override val registerStartAddress: UShort = 0x6000U

    override val registerEndAddress: UShort = 0xFFFFU

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override fun init() {
        reset(false)
    }

    override fun reset(softReset: Boolean) {
        writeRegister(0x6000U, 0U)
        writeRegister(0x8000U, 0U)
    }

    private inline fun updateState() {
        selectPrgPage(0U, ((regs[0].toInt() and 0x0F shl 1) or (regs[1].toInt() and 0x01)).toUShort())
        selectChrPage(0U, ((regs[0].toInt() and 0xF0 shr 1) or ((regs[1].toInt() and 0x70) shr 4)).toUShort())
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        regs[if (addr < 0x8000U) 0 else 1] = value
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readUByteArray("regs")?.copyInto(regs) ?: regs.fill(0U)
    }
}