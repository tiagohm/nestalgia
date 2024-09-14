package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_112

class Mapper112(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    @Volatile private var currentReg = 0
    @Volatile private var outerChrBank = 0
    private val registers = IntArray(8)

    override fun initialize() {
        mirroringType = VERTICAL

        addRegisterRange(0x4020, 0x5FFF, WRITE)

        selectPrgPage(2, -2)
        selectPrgPage(3, -1)
        updateState()
    }

    private fun updateState() {
        selectPrgPage(0, registers[0])
        selectPrgPage(1, registers[1])

        selectChrPage2x(0, registers[2])
        selectChrPage2x(1, registers[3])
        selectChrPage(4, registers[4] or (outerChrBank and 0x10 shl 4))
        selectChrPage(5, registers[5] or (outerChrBank and 0x20 shl 3))
        selectChrPage(6, registers[6] or (outerChrBank and 0x40 shl 2))
        selectChrPage(7, registers[7] or (outerChrBank and 0x80 shl 1))
    }

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE001) {
            0x8000 -> currentReg = value and 0x07
            0xA000 -> registers[currentReg] = value
            0xC000 -> outerChrBank = value
            0xE000 -> mirroringType = if (value.bit0) HORIZONTAL else VERTICAL
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("currentReg", currentReg)
        s.write("outerChrBank", outerChrBank)
        s.write("registers", registers)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        currentReg = s.readInt("currentReg")
        outerChrBank = s.readInt("outerChrBank")
        s.readIntArray("registers", registers)
    }
}
