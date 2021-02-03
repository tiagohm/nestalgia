package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_147

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Sachen147 : Mapper() {

    private val txChip = TxcChip(true)

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x8000U

    override val registerEndAddress: UShort = 0xFFFFU

    override val allowRegisterRead = true

    override fun init() {
        addRegisterRange(0x4020U, 0x5FFFU, MemoryOperation.ANY)
        removeRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.READ)

        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    private inline fun updateState() {
        val out = txChip.output.toUShort()
        selectPrgPage(0U, ((out and 0x20U) shr 4) or (out and 0x01U))
        selectChrPage(0U, (out and 0x1EU) shr 1)
    }

    override fun readRegister(addr: UShort): UByte {
        val value = if (addr.toInt() and 0x103 == 0x100) {
            val v = txChip.read(addr).toUInt()
            (((v and 0x3FU) shl 2) or ((v and 0xC0U) shr 6)).toUByte()
        } else {
            console.memoryManager.getOpenBus()
        }

        updateState()

        return value
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        txChip.write(addr, ((value and 0xFCU) shr 2) or ((value and 0x03U).toUInt() shl 6).toUByte())

        if (addr >= 0x8000U) {
            updateState()
        }
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