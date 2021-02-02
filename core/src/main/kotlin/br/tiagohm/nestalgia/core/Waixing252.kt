package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_252

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Waixing252 : Mapper() {

    private val chrReg = UByteArray(8)
    private lateinit var vrcIrq: VrcIrq

    override val prgPageSize = 0x2000U

    override val chrPageSize = 0x400U

    override fun init() {
        vrcIrq = VrcIrq(console)
        chrReg.fill(0U)

        selectPrgPage(2U, 0xFFFEU)
        selectPrgPage(3U, 0xFFFFU)
    }

    override fun processCpuClock() {
        vrcIrq.processCpuCycle()
    }

    private inline fun updateState() {
        for (i in 0..7) {
            // CHR needs to be writeable (according to Nestopia's source, and this does remove visual glitches from the game)
            setPpuMemoryMapping(
                (0x400 * i).toUShort(),
                (0x400 * i + 0x3FF).toUShort(),
                chrReg[i].toUShort(),
                ChrMemoryType.DEFAULT,
                MemoryAccessType.READ_WRITE,
            )
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr <= 0x8FFFU) {
            selectPrgPage(0U, value.toUShort())
        } else if (addr >= 0xA000U && addr <= 0xAFFFU) {
            selectPrgPage(1U, value.toUShort())
        } else if (addr >= 0xB000U && addr <= 0xEFFFU) {
            val shift = addr.toInt() and 0x4
            val bank = ((((addr.toInt() - 0xB000) shr 1) and 0x1800) or ((addr.toInt() shl 7) and 0x0400)) / 0x400
            chrReg[bank] =
                ((chrReg[bank].toInt() and (0xF0 shr shift)) or ((value.toInt() and 0x0F) shl shift)).toUByte()
            updateState()
        } else {
            when (addr.toInt() and 0xF00C) {
                0xF000 -> vrcIrq.setReloadValueNibble(value, false)
                0xF004 -> vrcIrq.setReloadValueNibble(value, true)
                0xF008 -> vrcIrq.setControlValue(value)
                0xF00C -> vrcIrq.acknowledgeIrq()
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chrReg", chrReg)
        s.write("vrcIrq", vrcIrq)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readUByteArray("chrReg")?.copyInto(chrReg) ?: chrReg.fill(0U)
        s.readSnapshot("vrcIrq")?.let { vrcIrq.restoreState(it) } ?: vrcIrq.reset(false)

        updateState()
    }
}