package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_220

class Kaiser7057(console: Console) : Mapper(console) {

    override val prgPageSize = 0x800

    override val chrPageSize = 0x2000

    private val regs = IntArray(8)

    override fun initialize() {
        selectChrPage(0, 0)

        updateState()
    }

    private fun updateState() {
        repeat(4) {
            addCpuMemoryMapping(0x6000 + 0x800 * it, 0x67FF + 0x800 * it, regs[4 + it], ROM)
            selectPrgPage(it, regs[it])
        }

        selectPrgPage4x(1, 0x34)
        selectPrgPage4x(2, 0x38)
        selectPrgPage4x(3, 0x3C)
    }

    private fun updatePrgReg(index: Int, value: Int, low: Boolean) {
        if (low) {
            regs[index] = regs[index] and 0xF0 or (value and 0x0F)
        } else {
            regs[index] = regs[index] and 0x0F or (value shl 4 and 0xF0)
        }

        updateState()
    }

    override fun writeRegister(addr: Int, value: Int) {
        val low = !addr.bit0

        when (addr and 0xF002) {
            0x8000, 0x8002, 0x9000, 0x9002 -> mirroringType = if (value.bit0) VERTICAL else HORIZONTAL
            0xB000 -> updatePrgReg(0, value, low)
            0xB002 -> updatePrgReg(1, value, low)
            0xC000 -> updatePrgReg(2, value, low)
            0xC002 -> updatePrgReg(3, value, low)
            0xD000 -> updatePrgReg(4, value, low)
            0xD002 -> updatePrgReg(5, value, low)
            0xE000 -> updatePrgReg(6, value, low)
            0xE002 -> updatePrgReg(7, value, low)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("regs", regs)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("regs", regs)

        updateState()
    }
}
