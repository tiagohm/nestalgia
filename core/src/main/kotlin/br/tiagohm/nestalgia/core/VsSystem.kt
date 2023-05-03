package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_099

class VsSystem : Mapper() {

    private var prgChrSelectBit: UByte = 0U

    override val prgPageSize = 0x2000U

    override val chrPageSize = 0x2000U

    override val workRamSize = 0x800U

    override fun reset(softReset: Boolean) {
        super.reset(softReset)
        updateMemoryAccess(0U)
    }

    fun updateMemoryAccess(bit: UByte) {
        val dualConsole = console.dualConsole

        if (console.isMaster && dualConsole != null) {
            val mapper = dualConsole.mapper!! as VsSystem

            // Give memory access to master CPU or slave CPU, based on "bit"
            if (privateSaveRamSize == 0U && privateWorkRamSize == 0U) {
                removeCpuMemoryMapping(0x6000U, 0x7FFFU)
                mapper.removeCpuMemoryMapping(0x6000U, 0x7FFFU)
            }

            for (i in 0U..3U) {
                val startAddr = (0x6000U + i * 0x800U).toUShort()
                val endAddr = (0x67FFU + i * 0x800U).toUShort()

                setCpuMemoryMapping(
                    startAddr,
                    endAddr,
                    if (hasBattery) Pointer(saveRam) else Pointer(workRam),
                    if (bit.toUInt() != 0U) MemoryAccessType.READ_WRITE else MemoryAccessType.NO_ACCESS
                )

                mapper.setCpuMemoryMapping(
                    startAddr,
                    endAddr,
                    if (hasBattery) Pointer(saveRam) else Pointer(workRam),
                    if (bit.toUInt() != 0U) MemoryAccessType.NO_ACCESS else MemoryAccessType.READ_WRITE
                )
            }
        }
    }
}