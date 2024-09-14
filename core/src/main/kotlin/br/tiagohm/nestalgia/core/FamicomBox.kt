package br.tiagohm.nestalgia.core

// http://kevtris.org/mappers/famicombox/index6.html

class FamicomBox(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val internalRamSize = MemoryManager.FAMICOM_BOX_INTERNAL_RAM_SIZE

    override val registerStartAddress = 0x5000

    override val registerEndAddress = 0x5FFF

    override val allowRegisterRead = true

    override val dipSwitchCount = 8

    private val regs = IntArray(8)

    override fun initialize() {
        regs[7] = 0xFF

        selectPrgPage(0, 0)
        selectPrgPage(1, 1)

        selectChrPage(0, 0)
    }

    override fun reset(softReset: Boolean) {
        repeat(7) {
            regs[it] = 0
        }
    }

    override fun readRegister(addr: Int): Int {
        return when (addr and 7) {
            0 -> {
                regs[0] = 0xFF
                regs[0]
            }
            1 -> 0
            2 -> dipSwitches
            3 -> 0x00
            4 -> 0
            5 -> 0
            6 -> 0
            7 -> 0x22
            else -> 0
        }
    }

    @Suppress("WhenWithOnlyElse")
    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 7) {
            // None of this is implemented.
            else -> return
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)
    }
}
