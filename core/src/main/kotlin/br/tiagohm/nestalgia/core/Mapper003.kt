package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_003

class Mapper003(console: Console) : CNROM(console) {

    override val hasBusConflicts
        get() = info.subMapperId == 2

    override fun writeRegister(addr: Int, value: Int) {
        selectChrPage(0, value)
    }
}
