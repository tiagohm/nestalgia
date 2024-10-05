package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_549

class Mapper549(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage4x(0, 0x02 shl 2)
        selectChrPage(0, 0)

        writeRegister(0x8000, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        addCpuMemoryMapping(0x6000, 0x7FFF, (addr shr 2) or (addr shr 3 and 0x04), PrgMemoryType.ROM)
    }
}
