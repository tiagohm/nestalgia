package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_136

@Suppress("NOTHING_TO_INLINE")
class Sachen136 : Mapper() {

    private val txChip = TxcChip(true)

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override val allowRegisterRead = true

    override fun initialize() {
        addRegisterRange(0x4020, 0x5FFF, MemoryOperation.ANY)
        removeRegisterRange(0x8000, 0xFFFF, MemoryOperation.READ)

        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    private inline fun updateState() {
        selectChrPage(0, txChip.output)
    }

    override fun readRegister(addr: Int): Int {
        val openBus = console.memoryManager.openBus()

        val value = if (addr and 0x103 == 0x100) {
            (openBus and 0xC0) or (txChip.read(addr) and 0x3F)
        } else {
            openBus
        }

        updateState()

        return value
    }

    override fun writeRegister(addr: Int, value: Int) {
        txChip.write(addr, value and 0x3F)
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
