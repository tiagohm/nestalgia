package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_099

class VsSystem : Mapper() {

    private var prgChrSelectBit = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val workRamSize = 0x800

    override fun initialize() {}

    override fun reset(softReset: Boolean) {
        super.reset(softReset)
        updateMemoryAccess(0)
    }

    fun updateMemoryAccess(bit: Int) {
        val dualConsole = console.dualConsole

        if (console.isMaster && dualConsole != null) {
            val mapper = dualConsole.mapper!! as VsSystem

            // Give memory access to master CPU or slave CPU, based on "bit".
            if (mSaveRamSize == 0 && mWorkRamSize == 0) {
                removeCpuMemoryMapping(0x6000, 0x7FFF)
                mapper.removeCpuMemoryMapping(0x6000, 0x7FFF)
            }

            repeat(4) {
                val startAddr = 0x6000 + it * 0x800
                val endAddr = 0x67FF + it * 0x800

                addCpuMemoryMapping(
                    startAddr,
                    endAddr,
                    if (hasBattery) Pointer(saveRam) else Pointer(workRam),
                    if (bit != 0) MemoryAccessType.READ_WRITE else MemoryAccessType.NO_ACCESS,
                )

                mapper.addCpuMemoryMapping(
                    startAddr,
                    endAddr,
                    if (hasBattery) Pointer(saveRam) else Pointer(workRam),
                    if (bit != 0) MemoryAccessType.NO_ACCESS else MemoryAccessType.READ_WRITE,
                )
            }
        }
    }
}
