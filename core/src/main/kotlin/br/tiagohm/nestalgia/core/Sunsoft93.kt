package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_093

class Sunsoft93(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(1, -1)
    }

    override fun writeRegister(addr: Int, value: Int) {
        selectPrgPage(0, value shr 4 and 0x07)

        if (value.bit0) {
            selectChrPage(0, 0)
        } else {
            removePpuMemoryMapping(0x0000, 0x1FFF)
        }
    }
}
