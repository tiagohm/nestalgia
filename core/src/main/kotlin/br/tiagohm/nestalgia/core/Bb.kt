package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_108

@Suppress("NOTHING_TO_INLINE")
class Bb : Mapper() {

    private var prgReg: UByte = 0U
    private var chrReg: UByte = 0U

    override val prgPageSize = 0x2000U

    override val chrPageSize = 0x2000U

    override fun init() {
        prgReg = 0xFFU
        chrReg = 0U

        selectPrgPage4x(0U, 0xFFFCU)

        updateState()
    }

    private inline fun updateState() {
        setCpuMemoryMapping(0x6000U, 0x7FFFU, prgReg.toShort(), PrgMemoryType.ROM)
        selectChrPage(0U, chrReg.toUShort())
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if ((addr.toInt() and 0x9000) == 0x8000 || addr >= 0xF000U) {
            // A version of Bubble Bobble expects writes to $F000+ to switch the PRG banks
            chrReg = value
            prgReg = value
        } else {
            // For ProWres
            chrReg = value and 0x01U
        }

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgReg", prgReg)
        s.write("chrReg", chrReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgReg = s.readUByte("prgReg") ?: 0xFFU
        chrReg = s.readUByte("chrReg") ?: 0U

        updateState()
    }
}