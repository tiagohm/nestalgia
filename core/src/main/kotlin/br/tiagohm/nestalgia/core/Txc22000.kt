package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_036

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Txc22000 : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x8000U

    override val registerEndAddress: UShort = 0xFFFFU

    override val allowRegisterRead = true

    private val txChip = TxcChip(false)
    private var chrBank: UByte = 0U

    override fun init() {
        addRegisterRange(0x4100U, 0x5FFFU, MemoryOperation.ANY)
        removeRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.READ)

        chrBank = 0U

        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    private inline fun updateState() {
        selectPrgPage(0U, (txChip.output and 0x03U).toUShort())
        selectChrPage(0U, chrBank.toUShort())
    }

    override fun readRegister(addr: UShort): UByte {
        val openBus = console.memoryManager.getOpenBus()

        val value = if (addr.toInt() and 0x103 == 0x100) {
            (openBus and 0xCFU) or ((txChip.read(addr).toUInt() shl 4) and 0x30U).toUByte()
        } else {
            openBus
        }

        updateState()

        return value
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr.toInt() and 0xF200 == 0x4200) {
            chrBank = value
        }

        txChip.write(addr, (value shr 4) and 0x03U)

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("txChip", txChip)
        s.write("chrBank", chrBank)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshot("txChip")?.let { txChip.restoreState(it) }
        chrBank = s.readUByte("chrBank") ?: 0U
    }
}