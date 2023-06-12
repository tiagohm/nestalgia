package br.tiagohm.nestalgia.core

import kotlin.random.Random

class Rt01 : Mapper() {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x800

    override val allowRegisterRead = true

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, 0)
        selectChrPage(0, 0)
        selectChrPage(1, 0)
        selectChrPage(2, 0)
        selectChrPage(3, 0)
    }

    override fun readRegister(addr: Int): Int {
        return if (addr in 0xCE80..0xCEFF || addr in 0xFE80..0xFEFF) {
            0xF2 or (Random.nextBits(8) and 0x0D)
        } else {
            internalRead(addr)
        }
    }
}
