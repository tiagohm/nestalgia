package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_252

class Waixing252 : Mapper() {

    private val chrReg = IntArray(8)
    private lateinit var vrcIrq: VrcIrq

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override fun initialize() {
        vrcIrq = VrcIrq(console)
        chrReg.fill(0)

        selectPrgPage(2, -2)
        selectPrgPage(3, -1)
    }

    override fun processCpuClock() {
        vrcIrq.processCpuCycle()
    }

    private fun updateState() {
        for (i in 0..7) {
            // CHR needs to be writeable (according to Nestopia's source,
            // and this does remove visual glitches from the game).
            addPpuMemoryMapping(
                0x400 * i,
                0x400 * i + 0x3FF,
                chrReg[i],
                ChrMemoryType.DEFAULT,
                MemoryAccessType.READ_WRITE,
            )
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr <= 0x8FFF) {
            selectPrgPage(0, value)
        } else if (addr in 0xA000..0xAFFF) {
            selectPrgPage(1, value)
        } else if (addr in 0xB000..0xEFFF) {
            val shift = addr and 0x4
            val bank = (((addr - 0xB000) shr 1 and 0x1800) or (addr shl 7 and 0x0400)) / 0x400
            chrReg[bank] = (chrReg[bank] and (0xF0 shr shift)) or (value and 0x0F shl shift)
            updateState()
        } else {
            when (addr and 0xF00C) {
                0xF000 -> vrcIrq.reloadValueNibble(value, false)
                0xF004 -> vrcIrq.reloadValueNibble(value, true)
                0xF008 -> vrcIrq.controlValue(value)
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

        s.readIntArrayOrFill("chrReg", chrReg, 0)
        s.readSnapshotable("vrcIrq", vrcIrq) { vrcIrq.reset(false) }

        updateState()
    }
}
