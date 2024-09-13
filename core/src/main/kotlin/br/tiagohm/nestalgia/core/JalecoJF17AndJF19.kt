package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_072
// https://wiki.nesdev.com/w/index.php/INES_Mapper_092

class JalecoJF17AndJF19(console: Console, private val jf19Mode: Boolean) : Mapper(console) {

    @Volatile private var prgFlag = false
    @Volatile private var chrFlag = false

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val hasBusConflicts = true

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, -1)

        selectChrPage(0, 0)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (!prgFlag && value.bit7) {
            if (jf19Mode) {
                selectPrgPage(1, value and 0x0F)
            } else {
                selectPrgPage(0, value and 0x07)
            }
        }

        if (!chrFlag && value.bit6) {
            selectChrPage(0, value and 0x0F)
        }

        prgFlag = value.bit7
        chrFlag = value.bit6
    }
}
