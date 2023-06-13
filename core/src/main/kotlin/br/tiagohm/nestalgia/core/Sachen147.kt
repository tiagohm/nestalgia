package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_147

@Suppress("NOTHING_TO_INLINE")
class Sachen147(console: Console) : Mapper(console) {

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
        val out = txChip.output
        selectPrgPage(0, (out and 0x20 shr 4) or (out and 0x01))
        selectChrPage(0, out and 0x1E shr 1)
    }

    override fun readRegister(addr: Int): Int {
        val value = if (addr and 0x103 == 0x100) {
            val v = txChip.read(addr)
            (v and 0x3F shl 2) or (v and 0xC0 shr 6)
        } else {
            console.memoryManager.openBus()
        }

        updateState()

        return value
    }

    override fun writeRegister(addr: Int, value: Int) {
        txChip.write(addr, (value and 0xFC shr 2) or (value and 0x03 shl 6))

        if (addr >= 0x8000) {
            updateState()
        }
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
