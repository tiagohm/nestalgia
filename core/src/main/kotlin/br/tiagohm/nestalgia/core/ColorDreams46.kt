package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_046

class ColorDreams46(console: Console) : Mapper(console) {

    private val regs = IntArray(2)

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override fun initialize() {
        reset(false)
    }

    override fun reset(softReset: Boolean) {
        writeRegister(0x6000, 0)
        writeRegister(0x8000, 0)
    }

    private fun updateState() {
        selectPrgPage(0, (regs[0] and 0x0F shl 1) or (regs[1] and 0x01))
        selectChrPage(0, (regs[0] and 0xF0 shr 1) or (regs[1] and 0x70 shr 4))
    }

    override fun writeRegister(addr: Int, value: Int) {
        regs[if (addr < 0x8000) 0 else 1] = value
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArrayOrFill("regs", regs, 0)
    }
}
