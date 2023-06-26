package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_179

class Henggedianzi179(console: Console) : Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        addRegisterRange(0x5000, 0x5FFF, WRITE)
        selectPrgPage(0, 0)
        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr >= 0x8000) {
            mirroringType = if (value.bit0) MirroringType.HORIZONTAL
            else MirroringType.VERTICAL
        } else {
            selectPrgPage(0, value shr 1)
        }
    }
}
