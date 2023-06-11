package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_087

class Mapper087 : JalecoJfxx() {

    override fun writeRegister(addr: Int, value: Int) {
        selectChrPage(0, (value and 0x01 shl 1) or (value and 0x02 shr 1))
    }
}
