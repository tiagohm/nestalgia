package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_101

class Mapper101(console: Console) : JalecoJFxx(console) {

    override fun writeRegister(addr: Int, value: Int) {
        selectChrPage(0, value)
    }
}
