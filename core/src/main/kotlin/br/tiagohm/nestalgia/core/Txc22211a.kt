package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_132

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
open class Txc22211a : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x8000U

    override val registerEndAddress: UShort = 0xFFFFU

    override val allowRegisterRead = true

    protected val txChip = TxcChip(false)

    override fun init() {
        addRegisterRange(0x4020U, 0x5FFFU, MemoryOperation.ANY)
        removeRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.READ)

        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    protected open fun updateState() {
        selectPrgPage(0U, if (txChip.output.bit2) 0x01U else 0x00U)
        selectChrPage(0U, (txChip.output and 0x03U).toUShort())
    }

    override fun readRegister(addr: UShort): UByte {
        val openBus = console.memoryManager.getOpenBus()

        val value = if (addr.toInt() and 0x103 == 0x100) {
            (openBus and 0xF0U) or (txChip.read(addr) and 0x0FU)
        } else {
            openBus
        }

        updateState()

        return value
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        txChip.write(addr, value and 0x0FU)
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("txChip", txChip)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshot("txChip")?.let { txChip.restoreState(it) } ?: txChip.reset(false)
    }
}