package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_036

class Txc22000(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override val allowRegisterRead = true

    private val txChip = TxcChip(false)
    private var chrBank = 0

    override fun initialize() {
        addRegisterRange(0x4100, 0x5FFF, MemoryOperation.ANY)
        removeRegisterRange(0x8000, 0xFFFF, MemoryOperation.READ)

        chrBank = 0

        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    private fun updateState() {
        selectPrgPage(0, txChip.output and 0x03)
        selectChrPage(0, chrBank)
    }

    override fun readRegister(addr: Int): Int {
        val openBus = console.memoryManager.openBus()

        val value = if (addr and 0x103 == 0x100) {
            (openBus and 0xCF) or (txChip.read(addr) shl 4 and 0x30)
        } else {
            openBus
        }

        updateState()

        return value
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr and 0xF200 == 0x4200) {
            chrBank = value
        }

        txChip.write(addr, value shr 4 and 0x03)

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("txChip", txChip)
        s.write("chrBank", chrBank)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("txChip", txChip) { txChip.reset(false) }
        chrBank = s.readInt("chrBank")
    }
}
