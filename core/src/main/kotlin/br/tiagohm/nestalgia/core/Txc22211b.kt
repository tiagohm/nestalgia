package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_172

@Suppress("NOTHING_TO_INLINE")
class Txc22211b : Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val registerStartAddress: UShort = 0x8000U

    override val registerEndAddress: UShort = 0xFFFFU

    override val allowRegisterRead = true

    private val txChip = TxcChip(true)

    override fun init() {
        addRegisterRange(0x4020U, 0x5FFFU, MemoryOperation.ANY)
        removeRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.READ)

        selectPrgPage(0U, 0U)
        selectChrPage(0U, 0U)
    }

    private inline fun updateState() {
        selectChrPage(0U, txChip.output.toUShort())
        mirroringType = if (txChip.invert) MirroringType.VERTICAL else MirroringType.HORIZONTAL
    }

    override fun readRegister(addr: UShort): UByte {
        val openBus = console.memoryManager.getOpenBus()

        val value = if (addr.toInt() and 0x103 == 0x100) {
            (openBus and 0xC0U) or convertValue(txChip.read(addr))
        } else {
            openBus
        }

        updateState()

        return value
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        txChip.write(addr, convertValue(value))
        if (addr.toInt() >= 0x8000) updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("txChip", txChip)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshot("txChip")?.let { txChip.restoreState(it) } ?: txChip.reset(false)
    }

    companion object {

        @JvmStatic
        private fun convertValue(v: UByte): UByte {
            val i = v.toInt()

            return (((i and 0x01) shl 5) or
                ((i and 0x02) shl 3) or
                ((i and 0x04) shl 1) or
                ((i and 0x08) shr 1) or
                ((i and 0x10) shr 3) or
                ((i and 0x20) shr 5)).toUByte()
        }
    }
}
