package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_132

open class Txc22211a(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override val allowRegisterRead = true

    protected val txChip = TxcChip(false)

    override fun initialize() {
        addRegisterRange(0x4020, 0x5FFF, MemoryOperation.ANY)
        removeRegisterRange(0x8000, 0xFFFF, MemoryOperation.READ)

        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    protected open fun updateState() {
        selectPrgPage(0, if (txChip.output.bit2) 0x01 else 0x00)
        selectChrPage(0, txChip.output and 0x03)
    }

    override fun readRegister(addr: Int): Int {
        val openBus = console.memoryManager.openBus()

        val value = if (addr and 0x103 == 0x100) {
            (openBus and 0xF0) or (txChip.read(addr) and 0x0F)
        } else {
            openBus
        }

        updateState()

        return value
    }

    override fun writeRegister(addr: Int, value: Int) {
        txChip.write(addr, value and 0x0F)
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("txChip", txChip)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("txChip", txChip) { txChip.reset(false) }
    }
}
