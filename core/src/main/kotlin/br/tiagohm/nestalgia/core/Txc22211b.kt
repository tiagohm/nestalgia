package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_172

class Txc22211b(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override val allowRegisterRead = true

    private val txChip = TxcChip(true)

    override fun initialize() {
        addRegisterRange(0x4020, 0x5FFF, READ_WRITE)
        removeRegisterRange(0x8000, 0xFFFF, READ)

        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    private fun updateState() {
        selectChrPage(0, txChip.output)

        mirroringType = if (txChip.invert) MirroringType.VERTICAL
        else MirroringType.HORIZONTAL
    }

    override fun readRegister(addr: Int): Int {
        val openBus = console.memoryManager.openBus()

        val value = if (addr and 0x103 == 0x100) {
            (openBus and 0xC0) or convertValue(txChip.read(addr))
        } else {
            openBus
        }

        updateState()

        return value
    }

    override fun writeRegister(addr: Int, value: Int) {
        txChip.write(addr, convertValue(value))
        if (addr >= 0x8000) updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("txChip", txChip)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("txChip", txChip) { txChip.reset(false) }
    }

    companion object {

        @JvmStatic
        private fun convertValue(v: Int): Int {
            return (v and 0x01 shl 5) or
                (v and 0x02 shl 3) or
                (v and 0x04 shl 1) or
                (v and 0x08 shr 1) or
                (v and 0x10 shr 3) or
                (v and 0x20 shr 5)
        }
    }
}
